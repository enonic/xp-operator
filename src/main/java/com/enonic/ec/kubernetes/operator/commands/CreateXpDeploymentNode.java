package com.enonic.ec.kubernetes.operator.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedKubernetesCommand;
import com.enonic.ec.kubernetes.operator.commands.builders.config.ConfigBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutablePodDisruptionBudgetSpecBuilder;
import com.enonic.ec.kubernetes.operator.commands.builders.spec.ImmutableStatefulSetSpecVolumes;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyConfigMap;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyPodDisruptionBudget;
import com.enonic.ec.kubernetes.operator.commands.kubectl.apply.ImmutableCommandApplyStatefulSet;
import com.enonic.ec.kubernetes.operator.commands.kubectl.scale.ImmutableCommandScaleStatefulSet;
import com.enonic.ec.kubernetes.operator.commands.plan.XpNodeDeploymentPlan;

@Value.Immutable
public abstract class CreateXpDeploymentNode
    extends Configuration
    implements CommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract OwnerReference ownerReference();

    protected abstract String namespace();

    protected abstract String nodeName();

    protected abstract XpNodeDeploymentPlan nodePlan();

    protected abstract Map<String, String> defaultLabels();

    protected abstract ConfigBuilder configBuilder();

    protected abstract String blobStorageName();

    private String podImageName()
    {
        return cfgStrFmt( "operator.deployment.xp.pod.imageTemplate", nodePlan().xpVersion() );
    }

    @Override
    public void addCommands( ImmutableCombinedKubernetesCommand.Builder commandBuilder )
    {
        Map<String, String> nodeLabels = nodePlan().node().nodeExtraLabels( defaultLabels() );
        Integer minAvailable = nodePlan().scale() / 2; // TODO: Reconsider

        if ( nodePlan().changeDisruptionBudget() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyPodDisruptionBudget.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( nodeName() ).
                labels( nodeLabels ).
                spec( ImmutablePodDisruptionBudgetSpecBuilder.builder().
                    minAvailable( minAvailable ).
                    matchLabels( nodeLabels ).
                    build().
                    spec() ).
                build() );
        }

        if ( nodePlan().changeConfigMap() )
        {
            commandBuilder.addCommand( ImmutableCommandApplyConfigMap.builder().
                client( defaultClient() ).
                ownerReference( ownerReference() ).
                namespace( namespace() ).
                name( nodeName() ).
                labels( nodeLabels ).
                data( configBuilder().create( nodePlan().node().config() ) ).
                build() );
        }

        if ( nodePlan().changeStatefulSet() )
        {
            List<EnvVar> podEnv = new LinkedList<>();

            for ( Map.Entry<String, String> e : nodePlan().node().env().entrySet() )
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
                    replicas( nodePlan().scale() ).
                    podImage( podImageName() ).
                    podEnv( podEnv ).
                    podResources( Map.of( "cpu", nodePlan().node().resources().cpu(), "memory", nodePlan().node().resources().memory() ) ).
                    indexDiskSize( nodePlan().node().resources().disks().get( "index" ) ).
                    snapshotDiskSize( nodePlan().node().resources().disks().get( "snapshots" ) ).
                    blobDiskPvcName( blobStorageName() ).
                    serviceName( nodeName() ).
                    configMapName( nodeName() ).
                    build().
                    spec() ).
                build() );
        }

        if ( nodePlan().changeScale() )
        {
            commandBuilder.addCommand( ImmutableCommandScaleStatefulSet.builder().
                client( defaultClient() ).
                namespace( namespace() ).
                name( nodeName() ).
                scale( nodePlan().scale() ).
                build() );
        }
    }
}
