package com.enonic.cloud.operator.v1alpha2xp7deployment;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.Pod;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFieldsPod;
import com.enonic.cloud.operator.helpers.HandlerStatus;

import static com.enonic.cloud.common.Configuration.cfgStr;

/**
 * This operator class updates Xp7Deployment status fields
 */
@Singleton
public class OperatorXp7DeploymentStatus
    extends HandlerStatus<Xp7Deployment, Xp7DeploymentStatus>
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Override
    protected InformerSearcher<Xp7Deployment> informerSearcher()
    {
        return searchers.xp7Deployment();
    }

    @Override
    protected Xp7DeploymentStatus getStatus( final Xp7Deployment resource )
    {
        return resource.getXp7DeploymentStatus();
    }

    @Override
    protected Doneable<Xp7Deployment> updateStatus( final Xp7Deployment resource, final Xp7DeploymentStatus newStatus )
    {
        return clients.xp7Deployments().crdClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            edit().
            withStatus( newStatus );
    }

    @Override
    protected Xp7DeploymentStatus pollStatus( final Xp7Deployment resource )
    {
        // Get current status
        Xp7DeploymentStatus currentStatus = resource.getXp7DeploymentStatus();

        // Get pods in deployment
        List<Pod> pods = searchers.pod().query().stream().
            filter( pod -> filterPods( resource, pod ) ).
            collect( Collectors.toList() );

        // Set pod fields
        currentStatus.setXp7DeploymentStatusFields( buildFields( pods ) );

        // Get expected number of pods
        int expectedNumberOfPods = expectedNumberOfPods( resource );

        // If pod count does not match
        if ( pods.size() != expectedNumberOfPods )
        {
            return currentStatus.
                withState( Xp7DeploymentStatus.State.PENDING ).
                withMessage( "Pod count mismatch" );
        }

        // If deployment is disabled
        if ( !resource.getXp7DeploymentSpec().getEnabled() )
        {
            return currentStatus.
                withState( Xp7DeploymentStatus.State.STOPPED ).
                withMessage( "OK" );
        }

        // Iterate over pods and check status
        List<String> waitingForPods = new LinkedList<>();
        for ( Xp7DeploymentStatusFieldsPod pod : currentStatus.
            getXp7DeploymentStatusFields().
            getXp7DeploymentStatusFieldsPods() )
        {
            if ( !pod.getPhase().equals( "Running" ) || !pod.getReady() )
            {
                waitingForPods.add( pod.getName() );
            }
        }

        // If we are still waiting
        if ( !waitingForPods.isEmpty() )
        {
            waitingForPods.sort( String::compareTo );
            return currentStatus.
                withState( Xp7DeploymentStatus.State.PENDING ).
                withMessage( String.format( "Waiting for pods: %s", waitingForPods ) );
        }

        // Return OK
        return currentStatus.
            withState( Xp7DeploymentStatus.State.RUNNING ).
            withMessage( "OK" );
    }

    private boolean filterPods( final Xp7Deployment deployment, final Pod pod )
    {
        return Objects.equals( pod.getMetadata().getLabels().get( cfgStr( "operator.charts.values.labelKeys.deployment" ) ),
                               deployment.getMetadata().getName() );
    }

    private Xp7DeploymentStatusFields buildFields( final List<Pod> pods )
    {
        List<Xp7DeploymentStatusFieldsPod> fieldPods = new LinkedList<>();
        for ( Pod pod : pods )
        {
            Optional<ContainerStatus> cs =
                pod.getStatus().getContainerStatuses().stream().filter( s -> s.getName().equals( "exp" ) ).findFirst();
            fieldPods.add( new Xp7DeploymentStatusFieldsPod().
                withName( pod.getMetadata().getName() ).
                withReady( cs.isPresent() && cs.get().getReady() ).
                withPhase( pod.getStatus().getPhase() ) );
        }
        return new Xp7DeploymentStatusFields().withXp7DeploymentStatusFieldsPods( fieldPods );
    }

    private int expectedNumberOfPods( final Xp7Deployment deployment )
    {
        if ( !deployment.getXp7DeploymentSpec().getEnabled() )
        {
            return 0;
        }

        return deployment.getXp7DeploymentSpec().
            getXp7DeploymentSpecNodeGroups().stream().
            mapToInt( Xp7DeploymentSpecNodeGroup::getReplicas ).
            sum();
    }
}
