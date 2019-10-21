package com.enonic.ec.kubernetes.operator.commands;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.CombinedKubeCommand;
import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubeCommand;
import com.enonic.ec.kubernetes.deployment.vhost.Vhost;
import com.enonic.ec.kubernetes.deployment.vhost.VhostPath;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyIngress;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyIssuer;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyNamespace;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyPodDisruptionBudget;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyService;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyStatefulSet;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutableIngressSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutableIssuerSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutablePodDisruptionBudgetSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutableServiceSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutableStatefulSetSpecNonClusteredSpec;
import com.enonic.ec.kubernetes.operator.commands.delete.ImmutableCommandDeleteConfigMap;
import com.enonic.ec.kubernetes.operator.commands.delete.ImmutableCommandDeleteIngress;
import com.enonic.ec.kubernetes.operator.commands.delete.ImmutableCommandDeleteIssuer;
import com.enonic.ec.kubernetes.operator.commands.delete.ImmutableCommandDeletePodDisruptionBudget;
import com.enonic.ec.kubernetes.operator.commands.delete.ImmutableCommandDeleteService;
import com.enonic.ec.kubernetes.operator.commands.delete.ImmutableCommandDeleteStatefulSet;
import com.enonic.ec.kubernetes.operator.commands.plan.ImmutableXpNodeDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.ImmutableXpVhostDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.XpNodeDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.XpNodeDeploymentPlan;
import com.enonic.ec.kubernetes.operator.commands.plan.XpVhostDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.XpVhostDeploymentPlan;
import com.enonic.ec.kubernetes.operator.commands.scale.ImmutableCommandScaleStatefulSet;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClient;

@Value.Immutable
public abstract class CreateXpDeployment
    implements Command<CombinedKubeCommand>
{
    protected abstract KubernetesClient defaultClient();

    protected abstract IssuerClient issuerClient();

    protected abstract Optional<XpDeploymentResource> oldResource();

    protected abstract XpDeploymentResource newResource();

    @Value.Derived
    protected XpDeploymentResource resource()
    {
        return newResource();
    }

    @Value.Derived
    protected boolean newDeployment()
    {
        return oldResource().isEmpty();
    }

    @Value.Derived
    protected XpNodeDeploymentDiff nodeDiff()
    {
        return ImmutableXpNodeDeploymentDiff.builder().
            oldDeployment( oldResource() ).
            newDeployment( newResource() ).
            build();
    }

    @Value.Derived
    protected XpVhostDeploymentDiff vHostDiff()
    {
        return ImmutableXpVhostDeploymentDiff.builder().
            oldDeployment( oldResource() ).
            newDeployment( newResource() ).
            build();
    }

    @Value.Derived
    protected OwnerReference ownerReference()
    {
        return new OwnerReference( resource().getApiVersion(), true, true, resource().getKind(), resource().getMetadata().getName(),
                                   resource().getMetadata().getUid() );
    }

    @Value.Derived
    protected String podImageName()
    {
        return "gbbirkisson/xp:" + resource().getSpec().xpVersion() + "-ubuntu"; // TODO: Fix
    }

    @Override
    public CombinedKubeCommand execute()
        throws Exception
    {
        String namespaceName = resource().getSpec().defaultNamespaceName();
        String defaultResourceName = resource().getSpec().defaultResourceName();
        Map<String, String> defaultLabels = resource().getSpec().defaultLabels();

        ImmutableCombinedKubeCommand.Builder commandBuilder = ImmutableCombinedKubeCommand.builder();

        if ( newDeployment() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyNamespace.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                name( namespaceName ).
                build() );

            // TODO: Create network policy
        }

        for ( XpNodeDeploymentPlan nodePlan : nodeDiff().deploymentPlans() )
        {
            createDeployNodeCommands( commandBuilder, namespaceName, defaultLabels, nodePlan );
        }

        for ( XpDeploymentResourceSpecNode oldNode : nodeDiff().nodesRemoved() )
        {
            createDeleteNodeCommands( commandBuilder, namespaceName, oldNode );
        }

        for ( XpVhostDeploymentPlan vhostPlan : vHostDiff().deploymentPlans() )
        {
            createDeployVhostCommands( commandBuilder, namespaceName, defaultLabels, vhostPlan );
        }

        for ( Vhost oldVHost : vHostDiff().vHostsRemoved() )
        {
            createDeleteVHostsCommands( commandBuilder, namespaceName, defaultResourceName, oldVHost );
        }

        return commandBuilder.build();
    }

    private void createDeployNodeCommands( final ImmutableCombinedKubeCommand.Builder commandBuilder, final String namespaceName,
                                           final Map<String, String> defaultLabels, final XpNodeDeploymentPlan nodePlan )
    {
        // TODO: Handle all nodetypes
        String nodeName = resource().getSpec().defaultResourceName( nodePlan.node().alias() );
        Map<String, String> nodeLabels = new HashMap<>( defaultLabels );
        nodeLabels.putAll( nodePlan.node().nodeAliasLabel() );

        if ( nodePlan.changeDisruptionBudget() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyPodDisruptionBudget.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( nodeName ).
                labels( nodeLabels ).
                spec( ImmutablePodDisruptionBudgetSpecBuilder.builder().
                    minAvailable( nodePlan.scale() / 2 ). // TODO: Reconsider
                    matchLabels( nodeLabels ).
                    build().
                    execute() ).
                build() );
        }

        if ( nodePlan.changeConfigMap() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( nodeName ).
                labels( nodeLabels ).
                data( nodePlan.node().config() ).
                build() );
        }

        if ( nodePlan.changeStatefulSet() )
        {
            List<EnvVar> podEnv = new LinkedList<>();

            for ( Map.Entry<String, String> e : nodePlan.node().env().entrySet() )
            {
                podEnv.add( new EnvVar( e.getKey(), e.getValue(), null ) );
            }

            commandBuilder.addCommand( ImmutableCommandApplyStatefulSet.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( nodeName ).
                labels( nodeLabels ).
                spec( ImmutableStatefulSetSpecNonClusteredSpec.builder().
                    podLabels( nodeLabels ).
                    replicas( nodePlan.scale() ).
                    podImage( podImageName() ).
                    podEnv( podEnv ).
                    podResources( Map.of( "cpu", nodePlan.node().resources().cpu(), "memory", nodePlan.node().resources().memory() ) ).
                    repoDiskSize( nodePlan.node().resources().disks().get( "repo" ) ).
                    snapshotDiskSize( nodePlan.node().resources().disks().get( "snapshots" ) ).
                    serviceName( nodeName ).
                    configMapName( nodeName ).
                    build().
                    execute() ).
                build() );
        }

        if ( nodePlan.changeScale() )
        {
            commandBuilder.addCommand( ImmutableCommandScaleStatefulSet.builder().
                client( defaultClient() ).
                namespace( namespaceName ).
                name( nodeName ).
                scale( nodePlan.scale() ).
                build() );
        }
    }

    private void createDeleteNodeCommands( final ImmutableCombinedKubeCommand.Builder commandBuilder, final String namespaceName,
                                           final XpDeploymentResourceSpecNode oldNode )
    {
        String nodeName = resource().getSpec().defaultResourceName( oldNode.alias() );

        commandBuilder.addCommand( ImmutableCommandDeleteStatefulSet.builder().
            client( defaultClient() ).
            namespace( namespaceName ).
            name( nodeName ).
            build() );

        commandBuilder.addCommand( ImmutableCommandDeletePodDisruptionBudget.builder().
            client( defaultClient() ).
            namespace( namespaceName ).
            name( nodeName ).
            build() );

        commandBuilder.addCommand( ImmutableCommandDeleteConfigMap.builder().
            client( defaultClient() ).
            namespace( namespaceName ).
            name( nodeName ).
            build() );
    }

    private void createDeployVhostCommands( final ImmutableCombinedKubeCommand.Builder commandBuilder, final String namespaceName,
                                            final Map<String, String> defaultLabels, final XpVhostDeploymentPlan vhostPlan )
        throws Exception
    {
        if ( vhostPlan.changeIssuer() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyIssuer.builder().
                client( issuerClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( vhostPlan.vhost().getVHostResourceName( resource() ) ).
                labels( defaultLabels ).
                spec( ImmutableIssuerSpecBuilder.builder().
                    certificate( vhostPlan.vhost().certificate().get() ).
                    build().
                    execute() ).
                build() );
        }

        // TODO: Delete issuer

        if ( vhostPlan.changeIngress() )
        {
            for ( VhostPath path : vhostPlan.newPaths() )
            {
                Map<String, String> serviceLabels = new HashMap<>();
                serviceLabels.putAll( defaultLabels );
                serviceLabels.putAll( path.getServiceSelectorLabels() );
                serviceLabels.putAll( path.getVhostLabels( vhostPlan.vhost() ) );
                commandBuilder.addCommand( ImmutableCommandApplyService.builder().
                    client( defaultClient() ).
                    ownerReference( ownerReference() ).
                    namespace( namespaceName ).
                    name( path.getPathResourceName( resource(), vhostPlan.vhost() ) ).
                    labels( serviceLabels ).
                    spec( ImmutableServiceSpecBuilder.builder().
                        selector( path.getServiceSelectorLabels() ).
                        build().
                        execute() ).
                    build() );
            }

            for ( VhostPath path : vhostPlan.pathsToDelete() )
            {
                commandBuilder.addCommand( ImmutableCommandDeleteService.builder().
                    client( defaultClient() ).
                    namespace( namespaceName ).
                    name( path.getPathResourceName( resource(), vhostPlan.vhost() ) ).
                    build() );
            }

            Map<String, String> serviceAnnotations = new HashMap<>();
            serviceAnnotations.put( "kubernetes.io/ingress.class", "nginx" );
            serviceAnnotations.put( "ingress.kubernetes.io/rewrite-target", "/" );
            serviceAnnotations.put( "nginx.ingress.kubernetes.io/configuration-snippet",
                                    "proxy_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;\n" +
                                        "      proxy_hide_header l5d-remote-ip;\n" + "      proxy_hide_header l5d-server-id;" );

            if ( vhostPlan.vhost().certificate().isPresent() )
            {
                serviceAnnotations.put( "nginx.ingress.kubernetes.io/ssl-redirect", "true" );
                serviceAnnotations.put( "certmanager.k8s.io/issuer", vhostPlan.vhost().getVHostResourceName( resource() ) );
            }
            Optional<String> certSecret = vhostPlan.vhost().certificate().isPresent()
                ? Optional.of( vhostPlan.vhost().getVHostResourceName( resource() ) )
                : Optional.empty();
            commandBuilder.addCommand( ImmutableCommandApplyIngress.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( vhostPlan.vhost().getVHostResourceName( resource() ) ).
                labels( defaultLabels ).
                annotations( serviceAnnotations ).
                spec( ImmutableIngressSpecBuilder.builder().
                    certificateSecretName( certSecret ).
                    vhost( vhostPlan.vhost() ).
                    resource( resource() ).
                    build().
                    execute() ).
                build() );
        }
    }

    private void createDeleteVHostsCommands( final ImmutableCombinedKubeCommand.Builder commandBuilder, final String namespaceName,
                                             final String defaultResourceName, final Vhost oldVHost )
    {
        commandBuilder.addCommand( ImmutableCommandDeleteIngress.builder().
            client( defaultClient() ).
            namespace( namespaceName ).
            name( oldVHost.getVHostResourceName( resource() ) ).
            build() );

        for ( VhostPath path : oldVHost.vhostPaths() )
        {
            commandBuilder.addCommand( ImmutableCommandDeleteService.builder().
                namespace( namespaceName ).
                name( path.getPathResourceName( resource(), oldVHost ) ).
                build() );
        }

        if ( oldVHost.certificate().isPresent() )
        {
            commandBuilder.addCommand( ImmutableCommandDeleteIssuer.builder().
                client( issuerClient() ).
                namespace( namespaceName ).
                name( oldVHost.getVHostResourceName( resource() ) ).
                build() );
        }
    }
}
