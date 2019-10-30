package com.enonic.ec.kubernetes.operator.commands;

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
import com.enonic.ec.kubernetes.deployment.xpdeployment.spec.SpecNode;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ConfigBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutablePodDisruptionBudgetSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutableStatefulSetSpecVolumes;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyPodDisruptionBudget;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyStatefulSet;
import com.enonic.ec.kubernetes.operator.commands.kubectl.scale.ImmutableCommandScaleStatefulSet;

@Value.Immutable
public abstract class CreateXpDeploymentNode
    extends Configuration
    implements CommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract OwnerReference ownerReference();

    protected abstract String namespace();

    protected abstract String nodeName();

    protected abstract DiffSpec diffSpec();

    protected abstract DiffSpecNode diffSpecNode();

    protected abstract Map<String, String> defaultLabels();

    protected abstract ConfigBuilder configBuilder();

    protected abstract String blobStorageName();

    protected abstract String snapshotsStorageName();

    private String podImageName()
    {
        return cfgStrFmt( "operator.deployment.xp.pod.imageTemplate", diffSpec().newValue().get().xpVersion() );
    }

    @Override
    public void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        SpecNode newNode = diffSpecNode().newValue().get();
        Map<String, String> nodeLabels = newNode.nodeExtraLabels( defaultLabels() );

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
                    minAvailable( newNode.minimumAvailable() ).
                    matchLabels( nodeLabels ).
                    build().
                    spec() ).
                build() );
        }

        boolean changeConfig = diffSpecNode().configChanged();
        if ( changeConfig )
        {
            commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( nodeName() ).
                labels( nodeLabels ).
                data( configBuilder().create( newNode ) ).
                build() );
        }

        boolean changeStatefulSet = diffSpecNode().envChanged() || diffSpecNode().resourcesChanged();
        if ( changeStatefulSet )
        {
            List<EnvVar> podEnv = new LinkedList<>();

            for ( Map.Entry<String, String> e : newNode.env().entrySet() )
            {
                podEnv.add( new EnvVar( e.getKey(), e.getValue(), null ) );
            }

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
                    serviceName( nodeName() ).
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
