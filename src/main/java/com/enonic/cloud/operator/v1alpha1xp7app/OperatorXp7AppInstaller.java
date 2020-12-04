package com.enonic.cloud.operator.v1alpha1xp7app;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatusFieldsAppInfo;
import com.enonic.cloud.operator.helpers.InformerEventHandler;
import com.enonic.cloud.operator.helpers.Xp7DeploymentInfo;

import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.kubernetes.Comparators.namespaceAndName;
import static com.enonic.cloud.kubernetes.Predicates.fieldsEquals;
import static com.enonic.cloud.kubernetes.Predicates.hasFinalizer;
import static com.enonic.cloud.kubernetes.Predicates.isDeleted;

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

    @Override
    public void onNewAdd( final Xp7App newResource )
    {
        installApp( newResource );
    }

    @Override
    public void onUpdate( final Xp7App oldResource, final Xp7App newResource )
    {
        // If url or sha512 has changed
        if ( fieldsEquals( oldResource, r -> r.getXp7AppSpec().getUrl(), r -> r.getXp7AppSpec().getSha512() ).
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
            filter( app -> app.getXp7AppStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo() == null ).
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
        if ( app.getXp7AppSpec().getUrl().startsWith( "http://localhost" ) )
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
                appInstall( app.getMetadata().getNamespace(), app.getXp7AppSpec().getUrl(), app.getXp7AppSpec().getSha512() );

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
        if ( app.getXp7AppStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo() == null )
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
            withXp7AppStatusFields( app.getXp7AppStatus().getXp7AppStatusFields() );

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
                getXp7AppStatus().
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

    private void updateAppStatus( Xp7App resource, Xp7AppStatus status )
    {
        if ( resource.getXp7AppStatus().hashCode() != status.hashCode() )
        {
            K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
                inNamespace( resource.getMetadata().getNamespace() ).
                withName( resource.getMetadata().getName() ).
                edit().
                withStatus( status ) );
        }
    }

    private boolean removeFinalizer( Xp7App resource )
    {
        List<String> finalizers = resource.getMetadata().getFinalizers();

        if ( finalizers.remove( cfgStr( "operator.charts.values.finalizers.app.uninstall" ) ) )
        {
            K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
                inNamespace( resource.getMetadata().getNamespace() ).
                withName( resource.getMetadata().getName() ).
                edit().withNewMetadataLike( resource.getMetadata() ).withFinalizers( finalizers ).endMetadata() );
            return true;
        }
        return false;
    }

    private boolean namespaceBeingTerminated( Xp7App app )
    {
        return clients.k8s().namespaces().withName( app.getMetadata().getNamespace() ).get().getMetadata().getDeletionTimestamp() != null;
    }
}
