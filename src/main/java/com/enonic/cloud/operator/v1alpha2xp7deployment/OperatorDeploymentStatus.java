package com.enonic.cloud.operator.v1alpha2xp7deployment;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.staller.TaskRunner;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFieldsPod;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatusFieldsPods;

import static com.enonic.cloud.common.Configuration.cfgIfBool;
import static com.enonic.cloud.common.Configuration.cfgLong;


public class OperatorDeploymentStatus
    implements Runnable
{
    @Inject
    Clients clients;

    @Inject
    InformerSearcher<Xp7Deployment> xp7DeploymentInformerSearcher;

    @Inject
    InformerSearcher<Pod> podInformerSearcher;

    @Inject
    TaskRunner taskRunner;

    @ConfigProperty(name = "operator.tasks.statusDelaySeconds")
    Long statusDelay;

    void onStartup( @Observes StartupEvent _ev )
    {
        cfgIfBool( "operator.status.enabled", () -> taskRunner.scheduleAtFixedRate( this, cfgLong( "operator.tasks.initialDelayMs" ),
                                                                                    cfgLong( "operator.tasks.deployment.status.periodMs" ),
                                                                                    TimeUnit.MILLISECONDS ) );
    }

    @Override
    public void run()
    {
        xp7DeploymentInformerSearcher.getStream().
            filter( deployment -> deployment.getMetadata().getDeletionTimestamp() == null ).
            filter( deployment ->
                        Duration.between( Instant.parse( deployment.getMetadata().getCreationTimestamp() ), Instant.now() ).getSeconds() >
                            statusDelay ).
            forEach( deployment -> updateStatus( deployment, pollStatus( deployment ) ) );
    }

    private Xp7DeploymentStatus pollStatus( final Xp7Deployment deployment )
    {
        List<Pod> pods = podInformerSearcher.getStream().
            filter( pod -> filterPods( deployment, pod ) ).
            collect( Collectors.toList() );

        Xp7DeploymentStatusFields fields = buildFields( pods );

        int expectedNumberOfPods = expectedNumberOfPods( deployment );

        if ( pods.size() != expectedNumberOfPods )
        {
            return new Xp7DeploymentStatus().
                withState( Xp7DeploymentStatus.State.PENDING ).
                withMessage( "Pod count mismatch" ).
                withXp7DeploymentStatusFields( fields );
        }

        if ( !deployment.getXp7DeploymentSpec().getEnabled() )
        {
            return new Xp7DeploymentStatus().
                withState( Xp7DeploymentStatus.State.STOPPED ).
                withMessage( "OK" ).
                withXp7DeploymentStatusFields( fields );
        }

        for ( Xp7DeploymentStatusFieldsPod s : fields.getXp7DeploymentStatusFieldsPods().getAdditionalProperties().values() )
        {
            if ( !s.getPhase().equals( "Running" ) || !s.getReady() )
            {
                return new Xp7DeploymentStatus().
                    withState( Xp7DeploymentStatus.State.PENDING ).
                    withMessage( "Waiting for pods" ).
                    withXp7DeploymentStatusFields( fields );
            }
        }

        return new Xp7DeploymentStatus().
            withState( Xp7DeploymentStatus.State.RUNNING ).
            withMessage( "OK" ).
            withXp7DeploymentStatusFields( fields );
    }

    private Xp7DeploymentStatusFields buildFields( final List<Pod> pods )
    {
        Xp7DeploymentStatusFieldsPods fieldPods = new Xp7DeploymentStatusFieldsPods();
        for ( Pod pod : pods )
        {
            Optional<ContainerStatus> cs =
                pod.getStatus().getContainerStatuses().stream().filter( s -> s.getName().equals( "exp" ) ).findFirst();
            fieldPods.setAdditionalProperty( pod.getMetadata().getName(), new Xp7DeploymentStatusFieldsPod().
                withPhase( pod.getStatus().getPhase() ).
                withReady( cs.isPresent() && cs.get().getReady() ) );
        }
        return new Xp7DeploymentStatusFields().withXp7DeploymentStatusFieldsPods( fieldPods );
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

    private int expectedNumberOfPods( final Xp7Deployment deployment )
    {
        if ( !deployment.getXp7DeploymentSpec().getEnabled() )
        {
            return 0;
        }

        return deployment.getXp7DeploymentSpec().
            getXp7DeploymentSpecNodeGroups().
            getAdditionalProperties().
            values().
            stream().
            mapToInt( Xp7DeploymentSpecNodeGroup::getReplicas ).
            sum();
    }


    private void updateStatus( final Xp7Deployment deployment, final Xp7DeploymentStatus status )
    {
        if ( status.equals( deployment.getXp7DeploymentStatus() ) )
        {
            return;
        }

        K8sLogHelper.logDoneable( clients.xp7Deployments().crdClient().
            inNamespace( deployment.getMetadata().getNamespace() ).
            withName( deployment.getMetadata().getName() ).
            edit().
            withStatus( status ) );
    }
}
