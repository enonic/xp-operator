package com.enonic.cloud.operator.v1alpha2xp7deployment;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.Pod;

import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFieldsPod;
import com.enonic.cloud.operator.helpers.StatusHandler;


public class OperatorDeploymentStatus
    extends StatusHandler<Xp7Deployment, Xp7DeploymentStatus>
{
    @Inject
    Clients clients;

    @Inject
    InformerSearcher<Xp7Deployment> xp7DeploymentInformerSearcher;

    @Inject
    InformerSearcher<Pod> podInformerSearcher;

    @Override
    protected InformerSearcher<Xp7Deployment> informerSearcher()
    {
        return xp7DeploymentInformerSearcher;
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
        Xp7DeploymentStatus currentStatus =
            resource.getXp7DeploymentStatus() != null ? resource.getXp7DeploymentStatus() : new Xp7DeploymentStatus().
                withState( Xp7DeploymentStatus.State.PENDING ).
                withMessage( "Created" );

        List<Pod> pods = podInformerSearcher.getStream().
            filter( pod -> filterPods( resource, pod ) ).
            collect( Collectors.toList() );

        currentStatus.setXp7DeploymentStatusFields( buildFields( pods ) );

        int expectedNumberOfPods = expectedNumberOfPods( resource );

        if ( pods.size() != expectedNumberOfPods )
        {
            return currentStatus.
                withState( Xp7DeploymentStatus.State.PENDING ).
                withMessage( "Pod count mismatch" );
        }

        if ( !resource.getXp7DeploymentSpec().getEnabled() )
        {
            return currentStatus.
                withState( Xp7DeploymentStatus.State.STOPPED ).
                withMessage( "OK" );
        }

        for ( Xp7DeploymentStatusFieldsPod s : currentStatus.
            getXp7DeploymentStatusFields().
            getXp7DeploymentStatusFieldsPods() )
        {
            if ( !s.getPhase().equals( "Running" ) || !s.getReady() )
            {
                return currentStatus.
                    withState( Xp7DeploymentStatus.State.PENDING ).
                    withMessage( "Waiting for pods" );
            }
        }

        return currentStatus.
            withState( Xp7DeploymentStatus.State.RUNNING ).
            withMessage( "OK" );
    }

    private boolean filterPods( final Xp7Deployment deployment, final Pod pod )
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
