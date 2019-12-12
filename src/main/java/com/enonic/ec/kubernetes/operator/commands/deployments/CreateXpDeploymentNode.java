package com.enonic.ec.kubernetes.operator.commands.deployments;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyConfigMap;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyPodDisruptionBudget;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyService;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyStatefulSet;
import com.enonic.ec.kubernetes.kubectl.scale.ImmutableCommandScaleStatefulSet;
import com.enonic.ec.kubernetes.operator.commands.deployments.config.ClusterConfigurator;
import com.enonic.ec.kubernetes.operator.commands.deployments.spec.ImmutablePodDisruptionBudgetSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.deployments.spec.ImmutableServiceSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.deployments.spec.ImmutableStatefulSetSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.deployments.volumes.VolumeBuilder;
import com.enonic.ec.kubernetes.operator.commands.deployments.volumes.VolumeTripletList;
import com.enonic.ec.kubernetes.operator.crd.deployment.diff.DiffSpec;
import com.enonic.ec.kubernetes.operator.crd.deployment.diff.DiffSpecNode;
import com.enonic.ec.kubernetes.operator.crd.deployment.spec.SpecNode;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CreateXpDeploymentNode
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract OwnerReference ownerReference();

    protected abstract String deploymentName();

    protected abstract String namespace();

    protected abstract String nodeId();

    protected abstract String nodeShortName();

    protected abstract String nodeFullName();

    protected abstract String serviceName();

    protected abstract DiffSpec diffSpec();

    protected abstract DiffSpecNode diffSpecNode();

    protected abstract Map<String, String> defaultLabels();

    protected abstract ClusterConfigurator clusterConfigurator();

    protected abstract int minimumAvailable();

    protected abstract VolumeBuilder volumeBuilder();

    protected abstract boolean nodeSharedConfigChanged();

    protected abstract EnvVar suPassHash();

    private String podImageName()
    {
        return cfgStrFmt( "operator.deployment.xp.pod.imageTemplate", diffSpec().newValue().get().xpVersion() );
    }

    private static Map<String, String> nodeExtraLabels( String nodeId, SpecNode node, Map<String, String> defaultLabels )
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

    @Override
    public void addCommands( ImmutableCombinedCommand.Builder commandBuilder )
    {
        SpecNode newNode = diffSpecNode().newValue().get();
        Map<String, String> nodeLabels = nodeExtraLabels( nodeId(), newNode, defaultLabels() );

        if ( diffSpec().isNew() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyService.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( nodeId() ).
                spec( ImmutableServiceSpecBuilder.builder().
                    selector( nodeLabels ).
                    putPorts( cfgStr( "operator.deployment.xp.port.main.name" ), cfgInt( "operator.deployment.xp.port.main.number" ) ).
                    build().
                    spec() ).
                build() );
        }

        int effectiveScale = diffSpec().newValue().get().enabled() ? newNode.replicas() : 0;
        boolean changeScale = diffSpec().enabledChanged() || diffSpecNode().replicasChanged() || diffSpec().isNew();

        if ( changeScale )
        {
            commandBuilder.addCommand( ImmutableCommandApplyPodDisruptionBudget.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( nodeId() ).
                labels( nodeLabels ).
                spec( ImmutablePodDisruptionBudgetSpecBuilder.builder().
                    minAvailable( minimumAvailable() ).
                    matchLabels( nodeLabels ).
                    build().
                    spec() ).
                build() );
        }

        boolean changeConfig =
            nodeSharedConfigChanged() || diffSpecNode().configChanged() || diffSpecNode().replicasChanged() || diffSpec().isNew();
        if ( changeConfig )
        {
            commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( nodeId() ).
                labels( configMapExtraLabels( nodeId(), nodeLabels ) ).
                data( Collections.emptyMap() ).
                build() );

            clusterConfigurator().addCommands( commandBuilder, nodeId(), newNode );
        }

        boolean changeStatefulSet =
            diffSpec().versionChanged() || diffSpecNode().envChanged() || diffSpecNode().resourcesChanged() || diffSpec().isNew();
        if ( changeStatefulSet )
        {
            List<EnvVar> podEnv = new LinkedList<>();
            podEnv.add( suPassHash() );

            for ( Map.Entry<String, String> e : newNode.env().entrySet() )
            {
                podEnv.add( new EnvVar( e.getKey(), e.getValue(), null ) );
            }

            ImmutableMap.Builder<String, String> podAnnotations = new ImmutableMap.Builder<>();
            cfgIfBool( "operator.extensions.linkerd.enabled", () -> podAnnotations.put( "linkerd.io/inject", "enabled" ) );

            VolumeTripletList volumeList =
                volumeBuilder().getVolumeTriplets( nodeId(), Optional.ofNullable( newNode.resources().disks().get( "index" ) ) );

            commandBuilder.addCommand( ImmutableCommandApplyStatefulSet.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( nodeId() ).
                labels( nodeLabels ).
                spec( ImmutableStatefulSetSpecBuilder.builder().
                    podLabels( nodeLabels ).
                    replicas( effectiveScale ).
                    podImage( podImageName() ).
                    podEnv( podEnv ).
                    podAnnotations( podAnnotations.build() ).
                    podResources( Map.of( "cpu", newNode.resources().cpu(), "memory", newNode.resources().memory() ) ).
                    volumeList( volumeList ).
                    serviceName( serviceName() ).
                    build().
                    spec() ).
                build() );
        }

        if ( !changeStatefulSet && changeScale )
        {
            commandBuilder.addCommand( ImmutableCommandScaleStatefulSet.builder().
                client( defaultClient() ).
                namespace( namespace() ).
                name( nodeId() ).
                scale( effectiveScale ).
                build() );
        }
    }
}