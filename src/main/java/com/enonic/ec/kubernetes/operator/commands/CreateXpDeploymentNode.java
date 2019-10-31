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
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.deployment.diff.DiffSpec;
import com.enonic.ec.kubernetes.deployment.diff.DiffSpecNode;
import com.enonic.ec.kubernetes.deployment.spec.SpecNode;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ConfigBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutablePodDisruptionBudgetSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutableStatefulSetSpecVolumes;
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

    protected abstract String namespace();

    protected abstract String nodeName();

    protected abstract String serviceName();

    protected abstract DiffSpec diffSpec();

    protected abstract DiffSpecNode diffSpecNode();

    protected abstract Map<String, String> defaultLabels();

    protected abstract ConfigBuilder configBuilder();

    protected abstract int minimumAvailable();

    protected abstract String blobStorageName();

    protected abstract String snapshotsStorageName();

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

//            commandBuilder.addCommand( ImmutableCommandApplyService.builder().
//                client( defaultClient() ).
//                ownerReference( ownerReference() ).
//                namespace( namespace() ).
//                name( nodeName() ).
//                labels( nodeLabels ).
//                spec( ImmutableServiceSpecBuilder.builder().
//                    selector( nodeLabels ).
//                    putPorts( cfgStr( "operator.deployment.xp.port.main.name" ), cfgInt( "operator.deployment.xp.port.main.number" ) ).
//                    publishNotReadyAddresses( true ).
//                    build().
//                    spec() ).
//                build() );

            commandBuilder.addCommand( ImmutableCommandApplyStatefulSet.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( nodeName() ).
                labels( nodeLabels ).
                spec( ImmutableStatefulSetSpecVolumes.builder().
                    podLabels( nodeLabels ).
                    replicas( effectiveScale ).
                    podImage( podImageName() ).
                    podEnv( podEnv ).
                    podResources( Map.of( "cpu", newNode.resources().cpu(), "memory", newNode.resources().memory() ) ).
                    indexDiskSize( Optional.ofNullable( newNode.resources().disks().get( "index" ) ) ).
                    snapshotsPvcName( Optional.ofNullable( newNode.isDataNode() ? snapshotsStorageName() : null ) ).
                    blobDiskPvcName( blobStorageName() ).
                    serviceName( serviceName() ).
                    configMapName( nodeName() ).
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
