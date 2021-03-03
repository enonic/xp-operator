package com.enonic.cloud.operator.v1alpha1xp7app;

import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.AppEvent;
import com.enonic.cloud.apis.xp.service.AppEventType;
import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.cloud.kubernetes.client.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.cloud.kubernetes.client.v1alpha1.xp7app.Xp7AppStatusFields;
import com.enonic.cloud.kubernetes.client.v1alpha1.xp7app.Xp7AppStatusFieldsAppInfo;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.operator.helpers.Xp7DeploymentInfo;

import static com.enonic.cloud.kubernetes.Predicates.inNamespace;
import static com.enonic.cloud.kubernetes.Predicates.isDeleted;

/**
 * This operator class updates Xp7App status fields
 */
@Singleton
public class OperatorXp7AppStatus
    implements Runnable, Consumer<AppEvent>
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    XpClientCache xpClientCache;

    @Inject
    Xp7DeploymentInfo xp7DeploymentInfo;

    @Override
    public void run()
    {
        // Ensure app listener in every namespace
        searchers.xp7Deployment().stream().
            filter( isDeleted().negate() ).
            map( d -> d.getMetadata().getNamespace() ).
            forEach( n -> {
                if ( xp7DeploymentInfo.xpRunning( n ) )
                {
                    xpClientCache.appAddListener( n, this );
                }
            } );
    }

    @Override
    public void accept( final AppEvent appEvent )
    {
        // Find app key and match with k8s resource
        Optional<Xp7App> optionalXp7App = matchEvent( appEvent );

        // If no match is found, exit
        if ( optionalXp7App.isEmpty() )
        {
            return;
        }
        Xp7App app = optionalXp7App.get();

        // If app has been uninstalled but not deleted in K8s
        if ( appEvent.type() == AppEventType.UNINSTALLED )
        {
            if ( app.getMetadata().getDeletionTimestamp() == null )
            {
                // Trigger reinstall
                reinstall( app );
            }
            return;
        }

        // Collect info
        AppInfo info = appEvent.info();
        Xp7AppStatusFieldsAppInfo oldState = app.getStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo();
        int oldStatusHash = app.getStatus().hashCode();

        // If version in XP does not match version in K8s
        if ( !info.version().equals( oldState.getVersion() ) )
        {
            // Trigger reinstall
            reinstall( app );
            return;
        }

        // Update status
        Xp7AppStatus newStatus = createNewStatus( info );

        // If status has changed
        if ( newStatus.hashCode() != oldStatusHash )
        {
            // Update status
            K8sLogHelper.logEdit( clients.xp7Apps().
                inNamespace( app.getMetadata().getNamespace() ).
                withName( app.getMetadata().getName() ), a -> {
                a.setStatus( newStatus );
                return a;
            } );
        }
    }

    private Optional<Xp7App> matchEvent( AppEvent appEvent )
    {
        return searchers.xp7App().stream().
            filter( inNamespace( appEvent.namespace() ) ).
            filter( a -> filterAppByKey( a, appEvent.key() ) ).
            findFirst();
    }

    private boolean filterAppByKey( final Xp7App app, final String key )
    {
        if ( app.getStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo() == null )
        {
            return false;
        }
        return app.getStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo().getKey().equals( key );
    }

    private Xp7AppStatus createNewStatus( final AppInfo info )
    {
        Xp7AppStatus.State state = null;
        String message = "OK";
        switch ( info.state() )
        {
            case "started":
                state = Xp7AppStatus.State.RUNNING;
                break;
            case "stopped":
                state = Xp7AppStatus.State.STOPPED;
                break;
            default:
                state = Xp7AppStatus.State.ERROR;
                message = String.format( "Invalid app state '%'", info.state() );
        }

        return new Xp7AppStatus().
            withState( state ).
            withMessage( message ).
            withXp7AppStatusFields( new Xp7AppStatusFields().
                withXp7AppStatusFieldsAppInfo( new Xp7AppStatusFieldsAppInfo().
                    withDescription( info.description() ).
                    withDisplayName( info.displayName() ).
                    withKey( info.key() ).
                    withModifiedTime( info.modifiedTime() ).
                    withState( info.state() ).
                    withUrl( info.url() ).
                    withVendorName( info.vendorName() ).
                    withVendorUrl( info.vendorUrl() ).
                    withVersion( info.version() ) ) );
    }

    private void reinstall( Xp7App app )
    {
        K8sLogHelper.logEdit( clients.xp7Apps().
            inNamespace( app.getMetadata().getNamespace() ).
            withName( app.getMetadata().getName() ), a -> {
            a.setStatus( null );
            return a;
        } );
    }
}
