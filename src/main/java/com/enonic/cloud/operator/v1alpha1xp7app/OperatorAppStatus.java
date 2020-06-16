package com.enonic.cloud.operator.v1alpha1xp7app;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.common.staller.TaskRunner;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatus;

import static com.enonic.cloud.common.Configuration.cfgIfBool;
import static com.enonic.cloud.common.Configuration.cfgLong;

@ApplicationScoped
public class OperatorAppStatus
    implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger( OperatorAppStatus.class );

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    Clients clients;

    @Inject
    InformerSearcher<Xp7App> xp7AppInformerSearcher;

    @Inject
    InformerSearcher<Xp7Deployment> xp7DeploymentInformerSearcher;

    @Inject
    XpClientCache xpClientCache;

    @Inject
    TaskRunner taskRunner;

    @ConfigProperty(name = "operator.tasks.statusDelaySeconds")
    Long statusDelay;

    void onStartup( @Observes StartupEvent _ev )
    {
        cfgIfBool( "operator.status.enabled", () -> taskRunner.scheduleAtFixedRate( this, cfgLong( "operator.tasks.initialDelayMs" ),
                                                                                    cfgLong( "operator.tasks.app.status.periodMs" ),
                                                                                    TimeUnit.MILLISECONDS ) );
    }

    @Override
    public void run()
    {
        xp7AppInformerSearcher.getStream().
            filter( app -> app.getMetadata().getDeletionTimestamp() == null ).
            filter( app -> Duration.between( Instant.parse( app.getMetadata().getCreationTimestamp() ), Instant.now() ).getSeconds() >
                statusDelay ).
            forEach( app -> updateStatus( app, pollStatus( app ) ) );
    }

    private Xp7AppStatus pollStatus( final Xp7App app )
    {
        Xp7AppStatus currentStatus =
            app.getXp7AppStatus() != null ? app.getXp7AppStatus() : new Xp7AppStatus().withXp7AppStatusFields( new Xp7AppStatusFields() );

        if ( currentStatus.getXp7AppStatusFields().getAppKey() == null )
        {
            return currentStatus.
                withState( Xp7AppStatus.State.PENDING ).
                withMessage( "Pending install" );
        }

        try
        {
            return checkXp( app, currentStatus );
        }
        catch ( Exception e )
        {
            log.warn( String.format( "Failed calling XP for app '%s' in NS '%s': %s", app.getMetadata().getName(),
                                     app.getMetadata().getNamespace(), e.getMessage() ) );
            return currentStatus.
                withState( Xp7AppStatus.State.ERROR ).
                withMessage( "Failed calling XP" );
        }
    }

    private Xp7AppStatus checkXp( final Xp7App app, final Xp7AppStatus currentStatus )
    {
        if ( !xpRunning( app ) )
        {
            return currentStatus.
                withState( Xp7AppStatus.State.PENDING ).
                withMessage( "Deployment not in RUNNING state" );
        }

        for ( AppInfo appInfo : xpClientCache.list( app ).
            get().
            applications() )
        {
            if ( appInfo.key().equals( currentStatus.getXp7AppStatusFields().getAppKey() ) )
            {
                switch ( appInfo.state() )
                {
                    case "started":
                        return currentStatus.
                            withState( Xp7AppStatus.State.RUNNING ).
                            withMessage( "Started" );
                    case "stopped":
                        return currentStatus.
                            withState( Xp7AppStatus.State.STOPPED ).
                            withMessage( "Stopped" );
                    default:
                        return currentStatus.
                            withState( Xp7AppStatus.State.ERROR ).
                            withMessage( "Invalid app state" );
                }
            }
        }

        return currentStatus.
            withState( Xp7AppStatus.State.PENDING ).
            withMessage( "Pending install" );
    }

    private void updateStatus( final Xp7App app, final Xp7AppStatus status )
    {
        if ( status.equals( app.getXp7AppStatus() ) )
        {
            return;
        }

        K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
            inNamespace( app.getMetadata().getNamespace() ).
            withName( app.getMetadata().getName() ).
            edit().
            withStatus( status ) );
    }

    private boolean xpRunning( final Xp7App app )
    {
        Optional<Xp7Deployment> deployment = xp7DeploymentInformerSearcher.get( app.getMetadata().getNamespace() ).findFirst();
        if ( deployment.isEmpty() )
        {
            return false;
        }

        if ( deployment.get().getXp7DeploymentStatus() == null )
        {
            return false;
        }

        return deployment.get().getXp7DeploymentStatus().getState().equals( Xp7DeploymentStatus.State.RUNNING );
    }
}
