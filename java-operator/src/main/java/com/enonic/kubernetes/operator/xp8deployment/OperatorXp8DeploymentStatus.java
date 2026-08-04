package com.enonic.kubernetes.operator.xp8deployment;

import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.crd.v1.Xp8DeploymentStatus;
import com.enonic.kubernetes.crd.v1.xp8deploymentspec.NodeGroups;
import com.enonic.kubernetes.crd.v1.xp8deploymentstatus.Fields;
import com.enonic.kubernetes.crd.v1.xp8deploymentstatus.fields.Pods;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.kubernetes.Predicates.*;

/**
 * This operator class updates Xp8Deployment status fields
 */
@Singleton
public class OperatorXp8DeploymentStatus
    extends InformerEventHandler<Pod>
    implements Runnable, ApplicationEventListener<ServerStartupEvent>
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp8DeploymentStatus.class );

    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    Informers informers;

    @Override
    public void onApplicationEvent( ServerStartupEvent event )
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
        Optional<Xp8Deployment> xp8Deployment = searchers.xp8Deployment().find( inSameNamespaceAs( pod ) );

        if (xp8Deployment.isEmpty()) {
            return;
        }

        // Get current status
        Xp8DeploymentStatus currentStatus = xp8Deployment.get().getStatus();
        String oldStatusJson = Serialization.asJson( currentStatus );

        // Get all pods in deployment
        List<Pod> pods = searchers.pod().stream().
            filter( isEnonicManaged() ).
            filter( isPartOfDeployment( xp8Deployment.get() ) ).
            collect( Collectors.toList() );

        // Set pod fields
        currentStatus.setFields( buildFields( pods ) );

        // Get expected number of pods
        int expectedNumberOfPods = expectedNumberOfPods( xp8Deployment.get() );

        // If pod count does not match
        if (pods.size() != expectedNumberOfPods) {
            currentStatus.setState( Xp8DeploymentStatus.State.PENDING );
            currentStatus.setMessage( "Pod count mismatch" );
            updateOnChange( xp8Deployment.get(), oldStatusJson, currentStatus );
            return;
        }

        // If deployment is disabled
        if (!xp8Deployment.get().getSpec().getEnabled()) {
            currentStatus.setState( Xp8DeploymentStatus.State.STOPPED );
            currentStatus.setMessage( "OK" );
            updateOnChange( xp8Deployment.get(), oldStatusJson, currentStatus );
            return;
        }

        // Iterate over pods and check status
        List<String> waitingForPods = new LinkedList<>();
        for (Pods p : currentStatus.
            getFields().
            getPods()) {
            if (!p.getPhase().equals( "Running" ) || !p.getReady()) {
                waitingForPods.add( p.getName() );
            }
        }

        // If we are still waiting
        if (!waitingForPods.isEmpty()) {
            waitingForPods.sort( String::compareTo );
            currentStatus.setState( Xp8DeploymentStatus.State.PENDING );
            currentStatus.setMessage( String.format( "Waiting for pods: %s", waitingForPods.stream().collect( Collectors.joining( ", " ) ) ) );
            updateOnChange( xp8Deployment.get(), oldStatusJson, currentStatus );
            return;
        }

        // Return OK
        currentStatus.setState( Xp8DeploymentStatus.State.RUNNING );
        currentStatus.setMessage( "OK" );
        updateOnChange( xp8Deployment.get(), oldStatusJson, currentStatus );
    }

    private void updateOnChange( final Xp8Deployment resource, final String oldStatusJson, final Xp8DeploymentStatus newStatus )
    {
        if (!oldStatusJson.equals( Serialization.asJson( newStatus ) )) {
            log.debug("Set Deployment status : {} {} in {}", newStatus.getState(), resource.getMetadata().getName(), resource.getMetadata().getNamespace());

            K8sLogHelper.logEdit( clients.xp8Deployments().
                inNamespace( resource.getMetadata().getNamespace() ).
                withName( resource.getMetadata().getName() ), d -> {
                d.setStatus( newStatus );
                return d;
            } );
        }
    }

    private Fields buildFields( final List<Pod> pods )
    {
        List<Pods> fieldPods = new LinkedList<>();
        for (Pod pod : pods) {
            Optional<ContainerStatus> cs =
                pod.getStatus().getContainerStatuses().stream().filter( s -> s.getName().equals( "exp" ) ).findFirst();
            Pods fieldPod = new Pods();
            fieldPod.setName( pod.getMetadata().getName() );
            fieldPod.setReady( cs.isPresent() && cs.get().getReady() );
            fieldPod.setPhase( pod.getStatus().getPhase() );
            fieldPods.add( fieldPod );
        }
        Fields fields = new Fields();
        fields.setPods( fieldPods );
        return fields;
    }

    private int expectedNumberOfPods( final Xp8Deployment deployment )
    {
        if (!deployment.getSpec().getEnabled()) {
            return 0;
        }

        return deployment.getSpec().
            getNodeGroups().stream().
            mapToInt( NodeGroups::getReplicas ).
            sum();
    }
}
