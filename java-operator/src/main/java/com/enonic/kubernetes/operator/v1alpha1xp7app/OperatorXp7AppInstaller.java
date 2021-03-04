package com.enonic.kubernetes.operator.v1alpha1xp7app;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.kubernetes.apis.xp.XpClientCache;
import com.enonic.kubernetes.apis.xp.service.AppInfo;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppStatusFields;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppStatusFieldsAppInfo;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import com.enonic.kubernetes.operator.helpers.Xp7DeploymentInfo;

import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Comparators.namespaceAndName;
import static com.enonic.kubernetes.kubernetes.Predicates.fieldsEquals;
import static com.enonic.kubernetes.kubernetes.Predicates.hasFinalizer;
import static com.enonic.kubernetes.kubernetes.Predicates.isDeleted;

/**
 * This operator class installs/uninstalls apps in XP
 */
@Singleton
public class OperatorXp7AppInstaller
    extends InformerEventHandler<Xp7App>
    implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7AppInstaller.class );

    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    XpClientCache xpClientCache;

    @Inject
    Xp7DeploymentInfo xp7DeploymentInfo;

    public static Xp7AppStatusFields fieldsFromAppInfo( AppInfo appInfo )
    {
        if ( appInfo == null )
        {
            return null;
        }
        return new Xp7AppStatusFields().
            withXp7AppStatusFieldsAppInfo( new Xp7AppStatusFieldsAppInfo().
                withDescription( appInfo.description() ).
                withDisplayName( appInfo.displayName() ).
                withKey( appInfo.key() ).
                withModifiedTime( appInfo.modifiedTime() ).
                withState( appInfo.state() ).
                withUrl( appInfo.url() ).
                withVendorName( appInfo.vendorName() ).
                withVendorUrl( appInfo.vendorUrl() ).
                withVersion( appInfo.version() ) );
    }

    @Override
    public void onNewAdd( final Xp7App newResource )
    {
        installApp( newResource );
    }

    @Override
    public void onUpdate( final Xp7App oldResource, final Xp7App newResource )
    {
        // If url or sha512 has changed
        if ( fieldsEquals( oldResource, r -> r.getSpec().getUrl(), r -> r.getSpec().getSha512() ).
            negate().
            test( newResource ) )
        {
            // Try to reinstall app
            installApp( newResource );
        }

        // If app marked for deletion and has finalizers
        if ( isDeleted().and( hasFinalizer( cfgStr( "operator.charts.values.finalizers.app.uninstall" ) ) ).test( newResource ) )
        {
            uninstallApp( newResource );
        }
    }

    @Override
    public void onDelete( final Xp7App oldResource, final boolean b )
    {
        // Handled by finalizers
    }

    @Override
    public void run()
    {
        searchers.xp7App().stream().
            filter( isDeleted().negate() ).
            filter( hasFinalizer( cfgStr( "operator.charts.values.finalizers.app.uninstall" ) ) ).
            filter( app -> app.getStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo() == null ).
            sorted( namespaceAndName() ).
            forEach( this::installApp );

        searchers.xp7App().stream().
            filter( isDeleted() ).
            filter( hasFinalizer( cfgStr( "operator.charts.values.finalizers.app.uninstall" ) ) ).
            sorted( namespaceAndName() ).
            forEach( this::uninstallApp );
    }

    private synchronized void installApp( final Xp7App app )
    {
        // Create fail status
        Xp7AppStatus f = new Xp7AppStatus().
            withState( Xp7AppStatus.State.ERROR ).
            withXp7AppStatusFields( new Xp7AppStatusFields() );

        // This is never going to work, do not even try
        if ( app.getSpec().getUrl().startsWith( "http://localhost" ) )
        {
            updateAppStatus( app, f.withMessage( "Cannot install, app URL invalid" ) );
            return;
        }

        // Check if XP is running
        if ( !xp7DeploymentInfo.xpRunning( app.getMetadata().getNamespace() ) )
        {
            updateAppStatus( app, f.
                withState( Xp7AppStatus.State.PENDING ).
                withMessage( "Cannot install, XP not in RUNNING state" ) );
            return;
        }

        // Try to install
        try
        {
            AppInfo appInfo = xpClientCache.
                appInstall( app.getMetadata().getNamespace(), app.getSpec().getUrl(), app.getSpec().getSha512() );

            updateAppStatus( app, new Xp7AppStatus().
                withState( Xp7AppStatus.State.RUNNING ).
                withMessage( "OK" ).
                withXp7AppStatusFields( fieldsFromAppInfo( appInfo ) ) );
        }
        catch ( Exception e )
        {
            log.warn( String.format( "Failed installing app %s in NS %s: %s", app.getMetadata().getName(), app.getMetadata().getNamespace(),
                                     e.getMessage() ) );

            updateAppStatus( app, f.withMessage( "Cannot install, see operator logs" ) );
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private synchronized boolean uninstallApp( final Xp7App app )
    {
        // If whole namespace is being deleted just remove the app finalizer
        if ( namespaceBeingTerminated( app ) )
        {
            return removeFinalizer( app );
        }

        // If there is no app info, the app is not installed, hence just remove the app finalizer
        if ( app.getStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo() == null )
        {
            return removeFinalizer( app );
        }

        // If deployment is being deleted just remove the app finalizer
        if ( xp7DeploymentInfo.xpDeleted( app.getMetadata().getNamespace() ) )
        {
            return removeFinalizer( app );
        }

        // Create fail status
        Xp7AppStatus f = new Xp7AppStatus().
            withState( Xp7AppStatus.State.ERROR ).
            withXp7AppStatusFields( app.getStatus().getXp7AppStatusFields() );

        // XP is not running, nothing we can do
        if ( !xp7DeploymentInfo.xpRunning( app.getMetadata().getNamespace() ) )
        {
            updateAppStatus( app, f.withMessage( "Cannot uninstall, XP not in RUNNING state" ) );
            return false;
        }

        // Try to uninstall
        try
        {
            xpClientCache.appUninstall( app.getMetadata().getNamespace(), app.
                getStatus().
                getXp7AppStatusFields().
                getXp7AppStatusFieldsAppInfo().
                getKey() );
            return removeFinalizer( app );
        }
        catch ( Exception e )
        {
            log.warn(
                String.format( "Failed uninstalling app %s in NS %s: %s", app.getMetadata().getName(), app.getMetadata().getNamespace(),
                               e.getMessage() ) );
            updateAppStatus( app, f.withMessage( "Cannot uninstall, see operator logs" ) );
            return false;
        }
    }

    private void updateAppStatus( Xp7App resource, Xp7AppStatus status )
    {
        if ( resource.getStatus().hashCode() != status.hashCode() )
        {
            K8sLogHelper.logEdit( clients.xp7Apps().
                inNamespace( resource.getMetadata().getNamespace() ).
                withName( resource.getMetadata().getName() ), a -> {
                a.setStatus( status );
                return a;
            });
        }
    }

    private boolean removeFinalizer( Xp7App resource )
    {
        List<String> finalizers = resource.getMetadata().getFinalizers();

        if ( finalizers.remove( cfgStr( "operator.charts.values.finalizers.app.uninstall" ) ) )
        {
            K8sLogHelper.logEdit( clients.xp7Apps().
                inNamespace( resource.getMetadata().getNamespace() ).
                withName( resource.getMetadata().getName() ), a -> {
                a.getMetadata().setFinalizers( Collections.emptyList() );
                return a;
            });
            return true;
        }
        return false;
    }

    private boolean namespaceBeingTerminated( Xp7App app )
    {
        return clients.k8s().namespaces().withName( app.getMetadata().getNamespace() ).get().getMetadata().getDeletionTimestamp() != null;
    }
}