package com.enonic.ec.kubernetes.operator.commands;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResource;
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
import com.enonic.ec.kubernetes.operator.commands.plan.XpNodeDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.XpNodeDeploymentPlan;
import com.enonic.ec.kubernetes.operator.commands.plan.XpVhostDeploymentDiff;
import com.enonic.ec.kubernetes.operator.commands.plan.XpVhostDeploymentPlan;
import com.enonic.ec.kubernetes.operator.commands.scale.ImmutableCommandScaleStatefulSet;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClientProducer;

@Value.Immutable
public abstract class CommandDeployXp
    implements Command<Void>
{
    protected abstract KubernetesClient defaultClient();

    protected abstract IssuerClientProducer.IssuerClient issuerClient();

    protected abstract boolean newDeployment();

    protected abstract XpDeploymentResource resource();

    protected abstract XpNodeDeploymentDiff nodeDiff();

    protected abstract XpVhostDeploymentDiff vHostDiff();

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
    public Void execute()
        throws Exception
    {
        String namespaceName = resource().getSpec().fullProjectName();

        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder();

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
            createDeployNodeCommands( commandBuilder, namespaceName, resource().getSpec().fullAppName(),
                                      resource().getSpec().defaultLabels(), nodePlan );
        }

        // TODO: Delete old deployments

        for ( XpVhostDeploymentPlan vhostPlan : vHostDiff().deploymentPlans() )
        {
            createDeployVhostCommands( commandBuilder, namespaceName, resource().getSpec().fullAppName(),
                                       resource().getSpec().defaultLabels(), vhostPlan );
        }

        // TODO Delete old vHosts

        return commandBuilder.build().execute();
    }

    private void createDeployNodeCommands( final ImmutableCombinedCommand.Builder commandBuilder, final String namespaceName,
                                           final String fullAppName, final Map<String, String> defaultLabels,
                                           final XpNodeDeploymentPlan nodePlan )
    {
        // TODO: Handle all nodetypes

        String nodeName = fullAppName + "-" + nodePlan.node().alias();
        Map<String, String> nodeLabels = new HashMap<>( defaultLabels );
        nodeLabels.putAll( nodePlan.node().nodeAliasLabel() );
        Integer nodeScale = resource().getSpec().enabled() ? nodePlan.node().replicas() : 0;

        if ( nodePlan.changeDisruptionBudget( nodeScale ) )
        {
            commandBuilder.addCommand( ImmutableCommandApplyPodDisruptionBudget.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( nodeName ).
                labels( nodeLabels ).
                spec( ImmutablePodDisruptionBudgetSpecBuilder.builder().
                    minAvailable( nodeScale / 2 ). // TODO: Reconsider
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
            if ( nodePlan.node().env() != null )
            {
                for ( Map.Entry<String, String> e : nodePlan.node().env().entrySet() )
                {
                    podEnv.add( new EnvVar( e.getKey(), e.getValue(), null ) );
                }
            }

            commandBuilder.addCommand( ImmutableCommandApplyStatefulSet.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( nodeName ).
                labels( nodeLabels ).
                spec( ImmutableStatefulSetSpecNonClusteredSpec.builder().
                    podLabels( nodeLabels ).
                    replicas( nodeScale ).
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
                scale( nodeScale ).
                build() );
        }
    }

    private void createDeployVhostCommands( final ImmutableCombinedCommand.Builder commandBuilder, final String namespaceName,
                                            final String fullAppName, final Map<String, String> defaultLabels,
                                            final XpVhostDeploymentPlan vhostPlan )
        throws Exception
    {
        if ( vhostPlan.changeIssuer() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyIssuer.builder().
                client( issuerClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( vhostPlan.vhost().getVhostResourceName( fullAppName ) ).
                labels( defaultLabels ).
                spec( ImmutableIssuerSpecBuilder.builder().
                    certificate( vhostPlan.vhost().certificate() ).
                    build().
                    execute() ).
                build() );
        }

        if ( vhostPlan.changeIngress() )
        {
            for ( VhostPath path : vhostPlan.vhost().vhostPaths() )
            {
                Map<String, String> serviceLabels = new HashMap<>();
                serviceLabels.putAll( defaultLabels );
                serviceLabels.putAll( path.getVhostLabel() );
                commandBuilder.addCommand( ImmutableCommandApplyService.builder().
                    client( defaultClient() ).
                    ownerReference( ownerReference() ).
                    namespace( namespaceName ).
                    name( path.getPathResourceName( fullAppName, vhostPlan.vhost().host() ) ).
                    labels( serviceLabels ).
                    spec( ImmutableServiceSpecBuilder.builder().
                        selector( path.getVhostLabel() ).
                        build().
                        execute() ).
                    build() );
            }

            Map<String, String> serviceAnnotations = new HashMap<>();
            serviceAnnotations.put( "kubernetes.io/ingress.class", "nginx" );
            serviceAnnotations.put( "ingress.kubernetes.io/rewrite-target", "/" );
            serviceAnnotations.put( "nginx.ingress.kubernetes.io/configuration-snippet",
                                    "proxy_set_header l5d-dst-override $service_name.$namespace.svc.cluster.local:$service_port;\n" +
                                        "      proxy_hide_header l5d-remote-ip;\n" + "      proxy_hide_header l5d-server-id;" );

            if ( vhostPlan.vhost().certificate() != null )
            {
                serviceAnnotations.put( "nginx.ingress.kubernetes.io/ssl-redirect", "true" );
                serviceAnnotations.put( "certmanager.k8s.io/issuer", vhostPlan.vhost().getVhostResourceName( fullAppName ) );
            }

            commandBuilder.addCommand( ImmutableCommandApplyIngress.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespaceName ).
                name( vhostPlan.vhost().getVhostResourceName( fullAppName ) ).
                labels( defaultLabels ).
                annotations( serviceAnnotations ).
                spec( ImmutableIngressSpecBuilder.builder().
                    certificateSecretName(
                        vhostPlan.vhost().certificate() != null ? vhostPlan.vhost().getVhostResourceName( fullAppName ) : null ).
                    vhost( vhostPlan.vhost() ).
                    appFullName( fullAppName ).
                    build().
                    execute() ).
                build() );
        }
    }
}
