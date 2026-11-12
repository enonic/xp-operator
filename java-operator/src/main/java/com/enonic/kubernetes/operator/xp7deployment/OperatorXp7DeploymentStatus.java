package com.enonic.kubernetes.operator.xp7deployment;

import com.enonic.kubernetes.client.v1.xp7deployment.*;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.kubernetes.Predicates.*;

/**
 * This operator class updates Xp7Deployment status fields
 */
@ApplicationScoped
public class OperatorXp7DeploymentStatus
    extends InformerEventHandler<Pod>
    implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7DeploymentStatus.class );

    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    Informers informers;

    void onStart( @Observes StartupEvent ev )
    {
        listen( informers.podInformer() );
        scheduleSync( this );
    }

    @Override
    protected void onNewAdd( final Pod newPod )
    {
        onCondition(newPod, pod -> {
                    log.debug("onNew Pod: {} in {}", pod.getMetadata().getNamespace(), pod.getMetadata().getName());
                    this.handle(pod);
                }
                , isEnonicManaged());
    }

    @Override
    public void onUpdate( final Pod oldPod, final Pod newPod )
    {
        onCondition( newPod, pod -> {
            log.debug( "onUpdate Pod: {} in {}", pod.getMetadata().getNamespace(), pod.getMetadata().getName() );
            this.handle(pod);
        }, isEnonicManaged() );
    }

    @Override
    public void onDelete( final Pod oldPod, final boolean deletedFinalStateUnknown )
    {
        onCondition( oldPod, pod -> {
            log.debug( "onDelete Pod: {} in {}", pod.getMetadata().getNamespace(), pod.getMetadata().getName() );
            this.handle(pod);
        }, isEnonicManaged() );
    }

    /**
     * This is meant for status sync if operator for some reason did not receive events
     */
    @Override
    public void run()
    {
        log.debug( "Resync Pods" );

        // Pick one managed pod in each namespace and update status
        searchers.pod().stream().
            filter( isEnonicManaged() ).
            collect( Collectors.toMap( pod -> pod.getMetadata().getNamespace(), pod -> pod, ( p1, p2 ) -> p1 ) ).
            values().
            stream().
            forEach( this::handle );
    }

    private void handle( final Pod pod )
    {
        Optional<Xp7Deployment> xp7Deployment = searchers.xp7Deployment().find( inSameNamespaceAs( pod ) );

        if (xp7Deployment.isEmpty()) {
            return;
        }

        // Get current status
        Xp7DeploymentStatus currentStatus = xp7Deployment.get().getStatus();
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
        if (pods.size() != expectedNumberOfPods) {
            updateOnChange( xp7Deployment.get(), oldStatusHash, currentStatus.
                withState( Xp7DeploymentStatus.State.PENDING ).
                withMessage( "Pod count mismatch" ) );
            return;
        }

        // If deployment is disabled
        if (!xp7Deployment.get().getSpec().getEnabled()) {
            updateOnChange( xp7Deployment.get(), oldStatusHash, currentStatus.
                withState( Xp7DeploymentStatus.State.STOPPED ).
                withMessage( "OK" ) );
            return;
        }

        // Iterate over pods and check status
        List<String> waitingForPods = new LinkedList<>();
        for (Xp7DeploymentStatusFieldsPod p : currentStatus.
            getXp7DeploymentStatusFields().
            getXp7DeploymentStatusFieldsPods()) {
            if (!p.getPhase().equals( "Running" ) || !p.getReady()) {
                waitingForPods.add( p.getName() );
            }
        }

        // If we are still waiting
        if (!waitingForPods.isEmpty()) {
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
        if (oldStatusHash != newStatus.hashCode()) {
            log.debug("Set Deployment status : {} {} in {}", newStatus.getState(), resource.getMetadata().getName(), resource.getMetadata().getNamespace());

            K8sLogHelper.logEdit( clients.xp7Deployments().
                inNamespace( resource.getMetadata().getNamespace() ).
                withName( resource.getMetadata().getName() ), d -> {
                d.setStatus( newStatus );
                return d;
            } );
        }
    }

    private Xp7DeploymentStatusFields buildFields( final List<Pod> pods )
    {
        List<Xp7DeploymentStatusFieldsPod> fieldPods = new LinkedList<>();
        for (Pod pod : pods) {
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
        if (!deployment.getSpec().getEnabled()) {
            return 0;
        }

        return deployment.getSpec().
            getXp7DeploymentSpecNodeGroups().stream().
            mapToInt( Xp7DeploymentSpecNodeGroup::getReplicas ).
            sum();
    }
}
