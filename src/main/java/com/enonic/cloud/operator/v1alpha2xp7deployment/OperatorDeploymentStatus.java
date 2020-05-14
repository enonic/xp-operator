package com.enonic.cloud.operator.v1alpha2xp7deployment;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.staller.TaskRunner;
import com.enonic.cloud.kubernetes.caches.PodCache;
import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7DeploymentCache;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.status.CrdStatusState;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.ImmutableV1alpha2Xp7DeploymentStatus;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.ImmutableV1alpha2Xp7DeploymentStatusFields;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.ImmutableV1alpha2Xp7DeploymentStatusFieldsPod;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentSpecNode;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentStatus;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentStatusFields;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentStatusFieldsPod;

import static com.enonic.cloud.common.Configuration.cfgIfBool;
import static com.enonic.cloud.common.Configuration.cfgLong;


public class OperatorDeploymentStatus
    implements Runnable
{
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CrdClient crdClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    V1alpha2Xp7DeploymentCache v1alpha2Xp7DeploymentCache;

    @Inject
    PodCache podCache;

    @Inject
    TaskRunner taskRunner;

    void onStartup( @Observes StartupEvent _ev )
    {
        cfgIfBool( "operator.status.enabled", () -> taskRunner.scheduleAtFixedRate( this, cfgLong( "operator.tasks.initialDelayMs" ), cfgLong( "operator.tasks.deployment.status.periodMs" ), TimeUnit.MILLISECONDS ) );
    }

    @Override
    public void run()
    {
        v1alpha2Xp7DeploymentCache.getStream().
            filter( deployment -> deployment.getMetadata().getDeletionTimestamp() == null ).
            forEach( deployment -> updateStatus( deployment, pollStatus( deployment ) ) );
    }

    private V1alpha2Xp7DeploymentStatus pollStatus( final V1alpha2Xp7Deployment deployment )
    {
        List<Pod> pods = podCache.getStream().
            filter( pod -> filterPods( deployment, pod ) ).
            collect( Collectors.toList() );

        V1alpha2Xp7DeploymentStatusFields fields = buildFields( pods );

        ImmutableV1alpha2Xp7DeploymentStatus.Builder statusBuilder = ImmutableV1alpha2Xp7DeploymentStatus.builder().
            fields( fields );

        int expectedNumberOfPods = expectedNumberOfPods( deployment );

        if ( pods.size() != expectedNumberOfPods )
        {
            return statusBuilder.
                state( CrdStatusState.PENDING ).
                message( "Pod count mismatch" ).
                build();
        }

        if ( !deployment.getSpec().enabled() )
        {
            return statusBuilder.
                state( CrdStatusState.STOPPED ).
                message( "OK" ).
                build();
        }

        for ( V1alpha2Xp7DeploymentStatusFieldsPod s : fields.pods().values() )
        {
            if ( !s.phase().equals( "Running" ) || !s.ready() )
            {
                return statusBuilder.
                    state( CrdStatusState.PENDING ).
                    message( "Waiting for pods" ).
                    build();
            }
        }

        return statusBuilder.
            state( CrdStatusState.RUNNING ).
            message( "OK" ).
            build();
    }

    private V1alpha2Xp7DeploymentStatusFields buildFields( final List<Pod> pods )
    {
        ImmutableV1alpha2Xp7DeploymentStatusFields.Builder fieldsBuilder = ImmutableV1alpha2Xp7DeploymentStatusFields.builder();
        for ( Pod pod : pods )
        {
            Optional<ContainerStatus> cs =
                pod.getStatus().getContainerStatuses().stream().filter( s -> s.getName().equals( "exp" ) ).findFirst();
            fieldsBuilder.putPods( pod.getMetadata().getName(), ImmutableV1alpha2Xp7DeploymentStatusFieldsPod.builder().
                phase( pod.getStatus().getPhase() ).
                ready( cs.isPresent() && cs.get().getReady() ).
                build() );
        }
        return fieldsBuilder.build();
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

    private int expectedNumberOfPods( final V1alpha2Xp7Deployment deployment )
    {
        if ( !deployment.getSpec().enabled() )
        {
            return 0;
        }

        return deployment.getSpec().
            nodeGroups().
            values().
            stream().
            mapToInt( V1alpha2Xp7DeploymentSpecNode::replicas ).
            sum();
    }


    private void updateStatus( final V1alpha2Xp7Deployment deployment, final V1alpha2Xp7DeploymentStatus status )
    {
        if ( status.equals( deployment.getStatus() ) )
        {
            return;
        }

        K8sLogHelper.logDoneable( crdClient.xp7Deployments().
            inNamespace( deployment.getMetadata().getNamespace() ).
            withName( deployment.getMetadata().getName() ).
            edit().
            withStatus( status ) );
    }
}
