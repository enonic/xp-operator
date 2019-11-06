package com.enonic.ec.kubernetes.operator.commands;

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
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.deployment.diff.DiffSpec;
import com.enonic.ec.kubernetes.deployment.diff.DiffSpecNode;
import com.enonic.ec.kubernetes.deployment.spec.SpecNode;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ConfigBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutablePodDisruptionBudgetSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutableStatefulSetSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.volumes.VolumeBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.volumes.VolumeTripletList;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyPodDisruptionBudget;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyStatefulSet;
import com.enonic.ec.kubernetes.operator.commands.kubectl.scale.ImmutableCommandScaleStatefulSet;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CreateXpDeploymentNode
    extends Configuration
    implements CommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract OwnerReference ownerReference();

    protected abstract String deploymentName();

    protected abstract String namespace();

    protected abstract String nodeName();

    protected abstract String serviceName();

    protected abstract DiffSpec diffSpec();

    protected abstract DiffSpecNode diffSpecNode();

    protected abstract Map<String, String> defaultLabels();

    protected abstract ConfigBuilder configBuilder();

    protected abstract int minimumAvailable();

    protected abstract VolumeBuilder volumeBuilder();

//    protected abstract Optional<String> sharedStorageName();

    private String podImageName()
    {
        return cfgStrFmt( "operator.deployment.xp.pod.imageTemplate", diffSpec().newValue().get().xpVersion() );
    }

    private static Map<String, String> nodeExtraLabels( SpecNode node, Map<String, String> defaultLabels )
    {
        Map<String, String> res = new HashMap<>( defaultLabels );
        res.putAll( node.nodeAliasLabel() );

        if ( node.isMasterNode() )
        {
            res.put( cfgStr( "operator.deployment.xp.labels.nodeType.master" ), "true" );
        }

        if ( node.isDataNode() )
        {
            res.put( cfgStr( "operator.deployment.xp.labels.nodeType.data" ), "true" );
        }

        if ( node.isFrontendNode() )
        {
            res.put( cfgStr( "operator.deployment.xp.labels.nodeType.frontend" ), "true" );
        }

        return res;
    }

    @Override
    public void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        SpecNode newNode = diffSpecNode().newValue().get();
        Map<String, String> nodeLabels = nodeExtraLabels( newNode, defaultLabels() );

        int effectiveScale = diffSpec().newValue().get().enabled() ? newNode.replicas() : 0;
        boolean changeScale = diffSpec().enabledChanged() || diffSpecNode().replicasChanged();

        if ( changeScale )
        {
            commandBuilder.addCommand( ImmutableCommandApplyPodDisruptionBudget.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( nodeName() ).
                labels( nodeLabels ).
                spec( ImmutablePodDisruptionBudgetSpecBuilder.builder().
                    minAvailable( minimumAvailable() ).
                    matchLabels( nodeLabels ).
                    build().
                    spec() ).
                build() );
        }

        boolean changeConfig = diffSpecNode().configChanged() || diffSpecNode().replicasChanged();
        if ( changeConfig )
        {
            commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( nodeName() ).
                labels( nodeLabels ).
                data( configBuilder().create( nodeName(), newNode ) ).
                build() );
        }

        boolean changeStatefulSet = diffSpec().versionChanged() || diffSpecNode().envChanged() || diffSpecNode().resourcesChanged();
        if ( changeStatefulSet )
        {
            List<EnvVar> podEnv = new LinkedList<>();

            for ( Map.Entry<String, String> e : newNode.env().entrySet() )
            {
                podEnv.add( new EnvVar( e.getKey(), e.getValue(), null ) );
            }

            ImmutableMap.Builder<String, String> podAnnotations = new ImmutableMap.Builder<>();
            cfgIfBool( "operator.extensions.linkerd.enabled", () -> {
                podAnnotations.put( "linkerd.io/inject", "enabled" );
            } );

            VolumeTripletList volumeList =
                volumeBuilder().getVolumeTriplets( nodeName(), Optional.ofNullable( newNode.resources().disks().get( "index" ) ) );

            commandBuilder.addCommand( ImmutableCommandApplyStatefulSet.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( nodeName() ).
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
                name( nodeName() ).
                scale( effectiveScale ).
                build() );
        }
    }
}
