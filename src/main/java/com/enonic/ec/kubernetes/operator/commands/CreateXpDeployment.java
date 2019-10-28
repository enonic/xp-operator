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

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedKubernetesCommand;
import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.deployment.vhost.VHost;
import com.enonic.ec.kubernetes.deployment.vhost.VHostPath;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyIngress;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyIssuer;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyNamespace;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyPodDisruptionBudget;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyPvc;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyService;
import com.enonic.ec.kubernetes.operator.commands.apply.ImmutableCommandApplyStatefulSet;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutableIngressSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutableIssuerSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutablePodDisruptionBudgetSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutablePvcSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutableServiceSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.ImmutableStatefulSetSpecVolumes;
import com.enonic.ec.kubernetes.operator.commands.delete.ImmutableCommandDeleteConfigMap;
import com.enonic.ec.kubernetes.operator.commands.delete.ImmutableCommandDeleteIngress;
import com.enonic.ec.kubernetes.operator.commands.delete.ImmutableCommandDeleteIssuer;
import com.enonic.ec.kubernetes.operator.commands.delete.ImmutableCommandDeletePodDisruptionBudget;
import com.enonic.ec.kubernetes.operator.commands.delete.ImmutableCommandDeleteService;
import com.enonic.ec.kubernetes.operator.commands.delete.ImmutableCommandDeleteStatefulSet;
import com.enonic.ec.kubernetes.operator.commands.plan.ImmutableXpNodeDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.ImmutableXpVHostDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.XpNodeDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.XpNodeDeploymentPlan;
import com.enonic.ec.kubernetes.operator.commands.plan.XpVHostDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.XpVHostDeploymentPlan;
import com.enonic.ec.kubernetes.operator.commands.scale.ImmutableCommandScaleStatefulSet;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClient;

@Value.Immutable
public abstract class CreateXpDeployment
    extends Configuration
    implements Command<CombinedKubernetesCommand>
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
    protected XpVHostDeploymentDiff vHostDiff()
    {
        return ImmutableXpVHostDeploymentDiff.builder().
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
        return String.format( cfgStr( "operator.deployment.xp.pod.imageTemplate" ), resource().getSpec().xpVersion() );
    }

    @Override
    public CombinedKubernetesCommand execute()
        throws Exception
    {
        String namespaceName = resource().getSpec().defaultNamespaceName();
        String defaultBlobStorageName = resource().getSpec().defaultResourceNameWithPreFix( "blob" );
        Map<String, String> defaultLabels = resource().getSpec().defaultLabels();

        ImmutableCombinedKubernetesCommand.Builder commandBuilder = ImmutableCombinedKubernetesCommand.builder();

        if ( newDeployment() )
        {
            // Create namespace
            commandBuilder.addCommand( ImmutableCommandApplyNamespace.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                name( namespaceName ).
                build() );

            // Create blob storage
            commandBuilder.addCommand( ImmutableCommandApplyPvc.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( defaultBlobStorageName ).
                labels( defaultLabels ).
                spec( ImmutablePvcSpecBuilder.builder().
                    size( resource().getSpec().sharedDisks().blob() ).
                    addAccessMode( "ReadWriteMany" ).
                    build().
                    execute() ).
                build() );

            // Create es discovery service
            commandBuilder.addCommand( ImmutableCommandApplyService.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( resource().getSpec().defaultResourceNameWithPostFix( "es", "discovery" ) ).
                labels( defaultLabels ).
                spec( ImmutableServiceSpecBuilder.builder().
                    selector( defaultLabels ).
                    putPorts( cfgStr( "operator.deployment.xp.port.es.discovery.name" ),
                              cfgInt( "operator.deployment.xp.port.es.discovery.number" ) ).
                    publishNotReadyAddresses( true ).
                    build().
                    execute() ).
                build() );

            // TODO: Create network policy
        }

        for ( XpNodeDeploymentPlan nodePlan : nodeDiff().deploymentPlans() )
        {
            createDeployNodeCommands( commandBuilder, namespaceName, defaultLabels, nodePlan, defaultBlobStorageName );
        }

        for ( XpDeploymentResourceSpecNode oldNode : nodeDiff().nodesRemoved() )
        {
            createDeleteNodeCommands( commandBuilder, namespaceName, oldNode );
        }

        for ( XpVHostDeploymentPlan vHostPlan : vHostDiff().deploymentPlans() )
        {
            createDeployVHostCommands( commandBuilder, namespaceName, defaultLabels, vHostPlan );
        }

        for ( VHost oldVHost : vHostDiff().vHostsRemoved() )
        {
            createDeleteVHostsCommands( commandBuilder, namespaceName, oldVHost );
        }

        return commandBuilder.build();
    }

    private void createDeployNodeCommands( final ImmutableCombinedKubernetesCommand.Builder commandBuilder, final String namespaceName,
                                           final Map<String, String> defaultLabels, final XpNodeDeploymentPlan nodePlan,
                                           final String defaultBlobStorageName )
    {
        // TODO: Handle all node types
        String nodeName = resource().getSpec().defaultResourceNameWithPostFix( nodePlan.node().alias() );
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
                data( enhanceConfig( nodePlan ) ).
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
                spec( ImmutableStatefulSetSpecVolumes.builder().
                    podLabels( nodeLabels ).
                    replicas( nodePlan.scale() ).
                    podImage( podImageName() ).
                    podEnv( podEnv ).
                    podResources( Map.of( "cpu", nodePlan.node().resources().cpu(), "memory", nodePlan.node().resources().memory() ) ).
                    indexDiskSize( nodePlan.node().resources().disks().get( "index" ) ).
                    snapshotDiskSize( nodePlan.node().resources().disks().get( "snapshots" ) ).
                    blobDiskPvcName( defaultBlobStorageName ).
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

    private Map<String, String> enhanceConfig( final XpNodeDeploymentPlan nodePlan )
    {
        return nodePlan.node().config();
    }

    private void createDeleteNodeCommands( final ImmutableCombinedKubernetesCommand.Builder commandBuilder, final String namespaceName,
                                           final XpDeploymentResourceSpecNode oldNode )
    {
        String nodeName = resource().getSpec().defaultResourceNameWithPostFix( oldNode.alias() );

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

    private void createDeployVHostCommands( final ImmutableCombinedKubernetesCommand.Builder commandBuilder, final String namespaceName,
                                            final Map<String, String> defaultLabels, final XpVHostDeploymentPlan vHostPlan )
        throws Exception
    {
        if ( vHostPlan.changeIssuer() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyIssuer.builder().
                client( issuerClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( vHostPlan.vhost().getVHostResourceName( resource() ) ).
                labels( defaultLabels ).
                spec( ImmutableIssuerSpecBuilder.builder().
                    certificate( vHostPlan.vhost().certificate().get() ).
                    build().
                    execute() ).
                build() );
        }

        // TODO: Delete issuer

        if ( vHostPlan.changeIngress() )
        {
            for ( VHostPath path : vHostPlan.newPaths() )
            {
                Map<String, String> serviceLabels = new HashMap<>();
                serviceLabels.putAll( defaultLabels );
                serviceLabels.putAll( path.getServiceSelectorLabels() );
                serviceLabels.putAll( path.getVHostLabels( vHostPlan.vhost() ) );
                commandBuilder.addCommand( ImmutableCommandApplyService.builder().
                    client( defaultClient() ).
                    ownerReference( ownerReference() ).
                    namespace( namespaceName ).
                    name( path.getPathResourceName( resource(), vHostPlan.vhost() ) ).
                    labels( serviceLabels ).
                    spec( ImmutableServiceSpecBuilder.builder().
                        selector( path.getServiceSelectorLabels() ).
                        putPorts( cfgStr( "operator.deployment.xp.port.main.name" ), cfgInt( "operator.deployment.xp.port.main.number" ) ).
                        build().
                        execute() ).
                    build() );
            }

            for ( VHostPath path : vHostPlan.pathsToDelete() )
            {
                commandBuilder.addCommand( ImmutableCommandDeleteService.builder().
                    client( defaultClient() ).
                    namespace( namespaceName ).
                    name( path.getPathResourceName( resource(), vHostPlan.vhost() ) ).
                    build() );
            }

            Map<String, String> serviceAnnotations = new HashMap<>();
            serviceAnnotations.put( "kubernetes.io/ingress.class", "nginx" );
            serviceAnnotations.put( "ingress.kubernetes.io/rewrite-target", "/" );
            serviceAnnotations.put( "nginx.ingress.kubernetes.io/configuration-snippet",
                                    "proxy_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;\n" +
                                        "      proxy_hide_header l5d-remote-ip;\n" + "      proxy_hide_header l5d-server-id;" );

            if ( vHostPlan.vhost().certificate().isPresent() )
            {
                serviceAnnotations.put( "nginx.ingress.kubernetes.io/ssl-redirect", "true" );
                serviceAnnotations.put( "certmanager.k8s.io/issuer", vHostPlan.vhost().getVHostResourceName( resource() ) );
            }
            Optional<String> certSecret = vHostPlan.vhost().certificate().isPresent()
                ? Optional.of( vHostPlan.vhost().getVHostResourceName( resource() ) )
                : Optional.empty();
            commandBuilder.addCommand( ImmutableCommandApplyIngress.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( vHostPlan.vhost().getVHostResourceName( resource() ) ).
                labels( defaultLabels ).
                annotations( serviceAnnotations ).
                spec( ImmutableIngressSpecBuilder.builder().
                    certificateSecretName( certSecret ).
                    vhost( vHostPlan.vhost() ).
                    resource( resource() ).
                    build().
                    execute() ).
                build() );
        }
    }

    private void createDeleteVHostsCommands( final ImmutableCombinedKubernetesCommand.Builder commandBuilder, final String namespaceName,
                                             final VHost oldVHost )
    {
        commandBuilder.addCommand( ImmutableCommandDeleteIngress.builder().
            client( defaultClient() ).
            namespace( namespaceName ).
            name( oldVHost.getVHostResourceName( resource() ) ).
            build() );

        for ( VHostPath path : oldVHost.vHostPaths() )
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
