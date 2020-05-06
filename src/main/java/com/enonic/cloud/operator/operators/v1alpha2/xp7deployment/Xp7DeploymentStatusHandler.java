package com.enonic.cloud.operator.operators.v1alpha2.xp7deployment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;

import com.enonic.cloud.operator.crd.CrdStatusState;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.ImmutableV1alpha2Xp7DeploymentStatus;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.ImmutableV1alpha2Xp7DeploymentStatusFields;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.ImmutableV1alpha2Xp7DeploymentStatusFieldsPod;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentSpecNode;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentStatus;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentStatusFields;
import com.enonic.cloud.operator.operators.common.StatusHandler;
import com.enonic.cloud.operator.operators.common.cache.Caches;

@Value.Immutable
public abstract class Xp7DeploymentStatusHandler
    extends StatusHandler<V1alpha2Xp7DeploymentStatusFields, V1alpha2Xp7DeploymentStatus, V1alpha2Xp7Deployment>
{
    @Override
    protected Stream<V1alpha2Xp7Deployment> getResourcesToUpdate( final Caches caches )
    {
        return caches().getDeploymentCache().getCollection().stream();
    }

    @Override
    protected V1alpha2Xp7DeploymentStatus createDefaultStatus( final V1alpha2Xp7Deployment r )
    {
        return ImmutableV1alpha2Xp7DeploymentStatus.
            builder().
            fields( ImmutableV1alpha2Xp7DeploymentStatusFields.builder().build() ).
            build();
    }

    @Override
    protected V1alpha2Xp7DeploymentStatus createNewStatus( final Caches caches, final V1alpha2Xp7Deployment deployment,
                                                           final V1alpha2Xp7DeploymentStatus oldStatus )
    {
        ImmutableV1alpha2Xp7DeploymentStatusFields.Builder fieldsBuilder =
            ImmutableV1alpha2Xp7DeploymentStatusFields.builder().from( oldStatus.fields() );

        List<Pod> deploymentPods = caches.getPodCache().
            getByNamespace( deployment.getMetadata().getNamespace() ).
            filter( pod -> filterPods( deployment, pod ) ).
            collect( Collectors.toList() );

        fieldsBuilder.pods( new HashMap<>() );

        CrdStatusState state = CrdStatusState.RUNNING;
        String message = "OK";

        for ( Pod pod : deploymentPods )
        {
            Optional<ContainerStatus> cs =
                pod.getStatus().getContainerStatuses().stream().filter( s -> s.getName().equals( "exp" ) ).findFirst();

            fieldsBuilder.putPods( pod.getMetadata().getName(), ImmutableV1alpha2Xp7DeploymentStatusFieldsPod.builder().
                phase( pod.getStatus().getPhase() ).
                ready( cs.isPresent() && cs.get().getReady() ).
                build() );

            if ( Arrays.asList( "Succeeded", "Failed", "Unknown" ).contains( pod.getStatus().getPhase() ) )
            {
                state = CrdStatusState.ERROR;
                message = String.format( "%s: Invalid pod phase '%s'", pod.getMetadata().getName(), pod.getStatus().getPhase() );
                continue;
            }

            if ( pod.getStatus().getPhase().equals( "Pending" ) )
            {
                state = CrdStatusState.PENDING;
                message = String.format( "%s: Pod pending", pod.getMetadata().getName() );
                continue;
            }

            if ( state != CrdStatusState.RUNNING )
            {
                continue;
            }

            if ( cs.isEmpty() )
            {
                state = CrdStatusState.ERROR;
                message = String.format( "%s: XP missing", pod.getMetadata().getName() );
            }
            else
            {
                if ( !cs.get().getReady() )
                {
                    state = CrdStatusState.PENDING;
                    message = String.format( "%s: XP starting", pod.getMetadata().getName() );
                }
            }
        }

        if ( deploymentPods.isEmpty() && !deployment.getSpec().enabled() )
        {
            state = CrdStatusState.STOPPED;
            message = null;
        }
        else if ( state == CrdStatusState.RUNNING )
        {
            int expectedNrPods = getExpectedNumberOfPods( deployment );
            if ( expectedNrPods != deploymentPods.size() )
            {
                state = CrdStatusState.ERROR;
                message = "Deployment replica mismatch";
            }
        }

        return ImmutableV1alpha2Xp7DeploymentStatus.builder().
            from( oldStatus ).
            state( state ).
            message( message ).
            fields( fieldsBuilder.build() ).
            build();
    }

    private boolean filterPods( final V1alpha2Xp7Deployment deployment, final Pod pod )
    {
        for ( Map.Entry<String, String> l : deployment.getMetadata().getLabels().entrySet() )
        {
            if ( !Objects.equals( l.getValue(), pod.getMetadata().getLabels().get( l.getKey() ) ) )
            {
                return false;
            }
        }
        return true;
    }

    private int getExpectedNumberOfPods( final V1alpha2Xp7Deployment deployment )
    {
        return deployment.getSpec().
            nodeGroups().
            values().
            stream().
            mapToInt( V1alpha2Xp7DeploymentSpecNode::replicas ).
            sum();
    }
}
