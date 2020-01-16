package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyService;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyStatefulSet;
import com.enonic.ec.kubernetes.operator.kubectl.scale.ImmutableCommandScaleStatefulSet;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.config.ClusterConfigurator;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.spec.ImmutableServiceSpecBuilder;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.spec.ImmutableStatefulSetSpecBuilder;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.volumes.VolumeBuilder;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.volumes.VolumeTripletList;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.Xp7DeploymentSpecNode;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.DiffXp7DeploymentSpec;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.DiffXp7DeploymentSpecNode;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.InfoXp7Deployment;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CreateXpDeploymentNode
    extends Configuration
    implements CombinedCommandBuilder
{
    private static Map<String, String> nodeExtraLabels( String nodeId, Xp7DeploymentSpecNode node, Map<String, String> defaultLabels )
    {
        Map<String, String> res = new HashMap<>( defaultLabels );
        res.put( cfgStr( "operator.deployment.xp.labels.pod.id" ), nodeId );
        res.put( cfgStr( "operator.deployment.xp.labels.pod.managed" ), "true" );

        if ( node.isMasterNode() )
        {
            res.put( cfgStr( "operator.deployment.xp.labels.pod.type.master" ), "true" );
        }

        if ( node.isDataNode() )
        {
            res.put( cfgStr( "operator.deployment.xp.labels.pod.type.data" ), "true" );
        }

        if ( node.isFrontendNode() )
        {
            res.put( cfgStr( "operator.deployment.xp.labels.pod.type.frontend" ), "true" );
        }

        return res;
    }

    private static Map<String, String> configMapExtraLabels( String nodeId, Map<String, String> defaultLabels )
    {
        Map<String, String> res = new HashMap<>( defaultLabels );
        res.put( cfgStr( "operator.deployment.xp.labels.config.managed" ), "true" );
        res.put( cfgStr( "operator.deployment.xp.labels.config.node" ), nodeId );
        return res;
    }

    protected abstract KubernetesClient defaultClient();

    protected abstract InfoXp7Deployment info();

    protected abstract String nodeId();

    protected abstract DiffXp7DeploymentSpec diffSpec();

    protected abstract DiffXp7DeploymentSpecNode diffSpecNode();

    protected abstract ClusterConfigurator clusterConfigurator();

    protected abstract VolumeBuilder volumeBuilder();

    protected abstract boolean nodeSharedConfigChanged();

    protected abstract EnvVar suPassHash();

    private String podImageName()
    {
        return cfgStrFmt( "operator.deployment.xp.pod.imageTemplate", diffSpec().newValue().get().xpVersion() );
    }

    @Override
    public void addCommands( ImmutableCombinedCommand.Builder commandBuilder )
    {
        Xp7DeploymentSpecNode node = diffSpecNode().newValue().get();
        Map<String, String> nodeLabels = nodeExtraLabels( nodeId(), node, info().defaultLabels() );

        // If this is new node, create a service pointing to it
        if ( diffSpec().isNew() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyService.builder().
                client( defaultClient() ).
                ownerReference( info().ownerReference() ).
                namespace( info().namespaceName() ).
                name( nodeId() ).
                spec( ImmutableServiceSpecBuilder.builder().
                    selector( nodeLabels ).
                    putPorts( cfgStr( "operator.deployment.xp.port.main.name" ), cfgInt( "operator.deployment.xp.port.main.number" ) ).
                    build().
                    spec() ).
                build() );
        }

        int effectiveScale = diffSpec().newValue().get().enabled() ? node.replicas() : 0;
        boolean changeScale = diffSpec().enabledChanged() || diffSpecNode().replicasChanged() || diffSpec().isNew();

//        if ( changeScale )
//        {
//            Integer minimumAvailable = info().defaultMinimumAvailable( node );
//            // TODO: Delete pod disruption budget if minimumAvailable == 0
//            commandBuilder.addCommand( ImmutableCommandApplyPodDisruptionBudget.builder().
//                client( defaultClient() ).
//                ownerReference( info().ownerReference() ).
//                namespace( info().namespaceName() ).
//                name( nodeId() ).
//                labels( nodeLabels ).
//                spec( ImmutablePodDisruptionBudgetSpecBuilder.builder().
//                    minAvailable( minimumAvailable ).
//                    matchLabels( nodeLabels ).
//                    build().
//                    spec() ).
//                build() );
//        }

        // If node is new or replicas have changed, update cluster config
        if ( diffSpec().isNew() || diffSpecNode().replicasChanged() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
                client( defaultClient() ).
                ownerReference( info().ownerReference() ).
                namespace( info().namespaceName() ).
                name( nodeId() ).
                labels( configMapExtraLabels( nodeId(), nodeLabels ) ).
                data( Collections.emptyMap() ).
                build() );

            clusterConfigurator().addCommands( commandBuilder, nodeId(), node );
        }

        // Create / Update stateful set if needed
        boolean changeStatefulSet =
            diffSpec().versionChanged() || diffSpecNode().envChanged() || diffSpecNode().resourcesChanged() || diffSpec().isNew();
        if ( changeStatefulSet )
        {
            Set<EnvVar> podEnv = new HashSet<>();
            podEnv.add( suPassHash() );

            for ( Map.Entry<String, String> e : node.env().entrySet() )
            {
                podEnv.add( new EnvVar( e.getKey(), e.getValue(), null ) );
            }

            ImmutableMap.Builder<String, String> podAnnotations = new ImmutableMap.Builder<>();
            cfgIfBool( "operator.extensions.linkerd.enabled", () -> podAnnotations.put( "linkerd.io/inject", "enabled" ) );

            VolumeTripletList volumeList =
                volumeBuilder().getVolumeTriplets( nodeId(), Optional.ofNullable( node.resources().disks().get( "index" ) ) );

            commandBuilder.addCommand( ImmutableCommandApplyStatefulSet.builder().
                client( defaultClient() ).
                ownerReference( info().ownerReference() ).
                namespace( info().namespaceName() ).
                name( nodeId() ).
                labels( nodeLabels ).
                spec( ImmutableStatefulSetSpecBuilder.builder().
                    podLabels( nodeLabels ).
                    replicas( effectiveScale ).
                    podImage( podImageName() ).
                    podEnv( podEnv ).
                    podAnnotations( podAnnotations.build() ).
                    podResources( Map.of( "cpu", node.resources().cpu(), "memory", node.resources().memory() ) ).
                    volumeList( volumeList ).
                    serviceName( info().allNodesServiceName() ).
                    clusterConfigurator( clusterConfigurator() ).
                    maxMemoryPercentage( node.isOnlyMaster() ? 75 : 50 ).
                    build().
                    spec() ).
                build() );
        }

        // If there is only a scale change, just update the scale
        if ( !changeStatefulSet && changeScale )
        {
            commandBuilder.addCommand( ImmutableCommandScaleStatefulSet.builder().
                client( defaultClient() ).
                namespace( info().namespaceName() ).
                name( nodeId() ).
                scale( effectiveScale ).
                build() );
        }
    }
}
