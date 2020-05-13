package com.enonic.cloud.operator.v1alpha1xp7app;

import java.util.Optional;
import java.util.TimerTask;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.common.staller.TaskRunner;
import com.enonic.cloud.kubernetes.caches.V1alpha1Xp7AppCache;
import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7DeploymentCache;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.status.CrdStatusState;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.ImmutableV1alpha1Xp7AppStatus;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.ImmutableV1alpha1Xp7AppStatusFields;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.V1alpha1Xp7AppStatus;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;

import static com.enonic.cloud.common.Configuration.cfgIfBool;

@ApplicationScoped
public class OperatorAppStatus
    extends TimerTask
{
    private static final Logger log = LoggerFactory.getLogger( OperatorAppStatus.class );

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CrdClient crdClient;

    @Inject
    V1alpha1Xp7AppCache v1alpha1Xp7AppCache;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    V1alpha2Xp7DeploymentCache v1alpha2Xp7DeploymentCache;

    @Inject
    XpClientCache xpClientCache;

    @Inject
    TaskRunner taskRunner;

    void onStartup( @Observes StartupEvent _ev )
    {
        cfgIfBool( "operator.status.enabled", () -> taskRunner.schedule( this ) );
    }

    @Override
    public void run()
    {
        v1alpha1Xp7AppCache.getStream().
            filter( app -> app.getMetadata().getDeletionTimestamp() == null ).
            forEach( app -> updateStatus( app, pollStatus( app ) ) );
    }

    private V1alpha1Xp7AppStatus pollStatus( final V1alpha1Xp7App app )
    {
        V1alpha1Xp7AppStatus currentStatus = app.getStatus() != null ? app.getStatus() : ImmutableV1alpha1Xp7AppStatus.builder().
            fields( ImmutableV1alpha1Xp7AppStatusFields.builder().build() ).
            build();

        if ( currentStatus.fields().appKey() == null && app.getSpec().options().bootstrap() )
        {
            currentStatus = getBuilder( currentStatus ).
                fields( ImmutableV1alpha1Xp7AppStatusFields.builder().
                    appKey( app.getSpec().options().appKey() ).
                    build() ).
                build();
        }

        if ( currentStatus.fields().appKey() == null )
        {
            if ( app.getSpec().options().bootstrap() )
            {
                return getBuilder( currentStatus ).
                    state( CrdStatusState.ERROR ).
                    message( "Missing app key" ).
                    build();
            }
            else
            {
                return getBuilder( currentStatus ).
                    state( CrdStatusState.PENDING ).
                    message( "Pending install" ).
                    build();
            }
        }

        try
        {
            return checkXp( app, currentStatus );
        }
        catch ( Exception e )
        {
            log.warn( String.format( "Failed calling XP for app '%s' in NS '%s': %s", app.getMetadata().getName(),
                                     app.getMetadata().getNamespace(), e.getMessage() ) );
            return getBuilder( currentStatus ).
                state( CrdStatusState.ERROR ).
                message( "Failed calling XP" ).
                build();
        }
    }

    private V1alpha1Xp7AppStatus checkXp( final V1alpha1Xp7App app, final V1alpha1Xp7AppStatus currentStatus )
    {
        if(!xpRunning( app )) {
            return getBuilder( currentStatus ).
                state( CrdStatusState.PENDING ).
                message( "Deployment not in RUNNING state" ).
                build();
        }

        for ( AppInfo appInfo : xpClientCache.list( app ).
            get().
            applications() )
        {
            if ( appInfo.key().equals( currentStatus.fields().appKey() ) )
            {
                ImmutableV1alpha1Xp7AppStatus.Builder builder = getBuilder( currentStatus );
                switch ( appInfo.state() )
                {
                    case "started":
                        return builder.
                            state( CrdStatusState.RUNNING ).
                            message( "Started" ).
                            build();
                    case "stopped":
                        return builder.
                            state( CrdStatusState.STOPPED ).
                            message( "Stopped" ).
                            build();
                    default:
                        return builder.
                            state( CrdStatusState.ERROR ).
                            message( "Invalid app state" ).
                            build();
                }
            }
        }

        return getBuilder( currentStatus ).
            state( CrdStatusState.PENDING ).
            message( "Pending install" ).
            build();
    }

    private void updateStatus( final V1alpha1Xp7App app, final V1alpha1Xp7AppStatus status )
    {
        if ( status.equals( app.getStatus() ) )
        {
            return;
        }

        K8sLogHelper.logDoneable( crdClient.xp7Apps().
            inNamespace( app.getMetadata().getNamespace() ).
            withName( app.getMetadata().getName() ).
            edit().
            withStatus( status ) );
    }

    private boolean xpRunning( final V1alpha1Xp7App app )
    {
        Optional<V1alpha2Xp7Deployment> deployment = v1alpha2Xp7DeploymentCache.get( app.getMetadata().getNamespace() ).findFirst();
        if ( deployment.isEmpty() )
        {
            return false;
        }

        if ( deployment.get().getStatus() == null )
        {
            return false;
        }

        return deployment.get().getStatus().state().equals( CrdStatusState.RUNNING );
    }

    private ImmutableV1alpha1Xp7AppStatus.Builder getBuilder( V1alpha1Xp7AppStatus status )
    {
        return ImmutableV1alpha1Xp7AppStatus.builder().from( status );
    }
}
