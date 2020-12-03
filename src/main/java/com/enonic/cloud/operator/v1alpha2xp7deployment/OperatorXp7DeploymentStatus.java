package com.enonic.cloud.operator.v1alpha2xp7deployment;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFieldsPod;
import com.enonic.cloud.operator.helpers.InformerEventHandler;
import com.enonic.cloud.operator.helpers.Xp7DeploymentInfo;

import static com.enonic.cloud.kubernetes.Predicates.isEnonicManaged;
import static com.enonic.cloud.kubernetes.Predicates.isPartOfDeployment;
import static com.enonic.cloud.kubernetes.Predicates.onCondition;

/**
 * This operator class updates Xp7Deployment status fields
 */
@Singleton
public class OperatorXp7DeploymentStatus
    extends InformerEventHandler<Pod>
    implements Runnable
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    Xp7DeploymentInfo xp7DeploymentInfo;

    @Override
    protected void onNewAdd( final Pod newPod )
    {
        onCondition( newPod, this::handle, isEnonicManaged() );
    }

    @Override
    public void onUpdate( final Pod oldPod, final Pod newPod )
    {
        onCondition( newPod, this::handle, isEnonicManaged() );
    }

    @Override
    public void onDelete( final Pod oldPod, final boolean deletedFinalStateUnknown )
    {
        onCondition( oldPod, this::handle, isEnonicManaged() );
    }

    /**
     * This is meant for status sync if operator for some reason did not receive events
     */
    @Override
    public void run()
    {
        // Pick one managed pod in each namespace and update status
        searchers.pod().stream().
            filter( isEnonicManaged() ).
            collect( Collectors.toMap( pod -> pod.getMetadata().getNamespace(), pod -> pod, ( p1, p2 ) -> p1 ) ).
            values().
            stream().
            forEach( this::handle );
    }

    private synchronized void handle( final Pod pod )
    {
        Optional<Xp7Deployment> xp7Deployment = xp7DeploymentInfo.get( pod );

        if ( xp7Deployment.isEmpty() )
        {
            return;
        }

        // Get current status
        Xp7DeploymentStatus currentStatus = xp7Deployment.get().getXp7DeploymentStatus();
        int oldStatusHash = currentStatus.hashCode();

        // Get all pods in deployment
        List<Pod> pods = searchers.pod().stream().
            filter( isEnonicManaged() ).
            filter( isPartOfDeployment( xp7Deployment.get() ) ).
            collect( Collectors.toList() );

        // Set pod fields
        currentStatus.setXp7DeploymentStatusFields( buildFields( pods ) );

        // Get expected number of pods
        int expectedNumberOfPods = expectedNumberOfPods( xp7Deployment.get() );

        // If pod count does not match
        if ( pods.size() != expectedNumberOfPods )
        {
            updateOnChange( xp7Deployment.get(), oldStatusHash, currentStatus.
                withState( Xp7DeploymentStatus.State.PENDING ).
                withMessage( "Pod count mismatch" ) );
            return;
        }

        // If deployment is disabled
        if ( !xp7Deployment.get().getXp7DeploymentSpec().getEnabled() )
        {
            updateOnChange( xp7Deployment.get(), oldStatusHash, currentStatus.
                withState( Xp7DeploymentStatus.State.STOPPED ).
                withMessage( "OK" ) );
            return;
        }

        // Iterate over pods and check status
        List<String> waitingForPods = new LinkedList<>();
        for ( Xp7DeploymentStatusFieldsPod p : currentStatus.
            getXp7DeploymentStatusFields().
            getXp7DeploymentStatusFieldsPods() )
        {
            if ( !p.getPhase().equals( "Running" ) || !p.getReady() )
            {
                waitingForPods.add( p.getName() );
            }
        }

        // If we are still waiting
        if ( !waitingForPods.isEmpty() )
        {
            waitingForPods.sort( String::compareTo );
            updateOnChange( xp7Deployment.get(), oldStatusHash, currentStatus.
                withState( Xp7DeploymentStatus.State.PENDING ).
                withMessage( String.format( "Waiting for pods: %s", waitingForPods.stream().collect( Collectors.joining( ", " ) ) ) ) );
            return;
        }

        // Return OK
        updateOnChange( xp7Deployment.get(), oldStatusHash, currentStatus.
            withState( Xp7DeploymentStatus.State.RUNNING ).
            withMessage( "OK" ) );
    }

    private void updateOnChange( final Xp7Deployment resource, final int oldStatusHash, final Xp7DeploymentStatus newStatus )
    {
        if ( oldStatusHash != newStatus.hashCode() )
        {
            K8sLogHelper.logDoneable(clients.xp7Deployments().crdClient().
                inNamespace( resource.getMetadata().getNamespace() ).
                withName( resource.getMetadata().getName() ).
                edit().
                withStatus( newStatus ));
        }
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
