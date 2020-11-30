package com.enonic.cloud.operator.v1alpha1xp7app;

import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.AppEvent;
import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatusFieldsAppInfo;
import com.enonic.cloud.operator.helpers.Xp7DeploymentInfo;

import static com.enonic.cloud.common.Utils.hasMetadataComparator;

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
        searchers.xp7Deployment().query().
            stream().
            sorted( hasMetadataComparator() ).
            map( d -> d.getMetadata().getNamespace() ).
            forEach( n -> {
                if ( xp7DeploymentInfo.xpRunning( n ) )
                {
                    xpClientCache.addEventListener( n, this );
                }
            } );
    }

    @Override
    public void accept( final AppEvent appEvent )
    {
        String key = appEvent.info() != null ? appEvent.info().key() : appEvent.uninstall().key();
        Optional<Xp7App> optionalXp7App = searchers.xp7App().query().
            inNamespace( appEvent.namespace() ).
            filter( a -> filterAppByKey( a, key ) ).
            get();

        if ( optionalXp7App.isEmpty() )
        {
            return;
        }

        Xp7App app = optionalXp7App.get();

        // If app has been uninstalled but not deleted in K8s
        if ( appEvent.uninstall() != null )
        {
            if ( app.getMetadata().getDeletionTimestamp() == null )
            {
                // Trigger reinstall
                K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
                    inNamespace( app.getMetadata().getNamespace() ).
                    withName( app.getMetadata().getName() ).
                    edit().
                    withStatus( null ) );
            }
            return;
        }

        AppInfo newState = appEvent.info();
        Xp7AppStatusFieldsAppInfo oldState = app.getXp7AppStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo();
        int oldStatusHash = app.getXp7AppStatus().hashCode();

        // If version in XP does not match version in K8s
        if ( !newState.version().equals( oldState.getVersion() ) )
        {
            // Trigger reinstall
            K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
                inNamespace( app.getMetadata().getNamespace() ).
                withName( app.getMetadata().getName() ).
                edit().
                withStatus( null ) );
            return;
        }

        Xp7AppStatus.State xp7AppState = "started".equals( newState.state() ) ? Xp7AppStatus.State.RUNNING : Xp7AppStatus.State.STOPPED;
        Xp7AppStatus newStatus = new Xp7AppStatus().
            withState( xp7AppState ).
            withMessage( "OK" ).
            withXp7AppStatusFields( new Xp7AppStatusFields().
                withXp7AppStatusFieldsAppInfo( new Xp7AppStatusFieldsAppInfo().
                    withDescription( newState.description() ).
                    withDisplayName( newState.displayName() ).
                    withKey( newState.key() ).
                    withModifiedTime( newState.modifiedTime() ).
                    withState( newState.state() ).
                    withUrl( newState.url() ).
                    withVendorName( newState.vendorName() ).
                    withVendorUrl( newState.vendorUrl() ).
                    withVersion( newState.version() ) ) );

        if ( newStatus.hashCode() != oldStatusHash )
        {
            K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
                inNamespace( app.getMetadata().getNamespace() ).
                withName( app.getMetadata().getName() ).
                edit().
                withStatus( newStatus ) );
        }
    }

    private boolean filterAppByKey( final Xp7App app, final String key )
    {
        if ( app.getXp7AppStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo() == null )
        {
            return false;
        }
        return app.getXp7AppStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo().getKey().equals( key );
    }
}
