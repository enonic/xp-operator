package com.enonic.cloud.operator.v1alpha1xp7app;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.apis.xp.service.AppInstallResponse;
import com.enonic.cloud.apis.xp.service.ImmutableAppInstallRequest;
import com.enonic.cloud.apis.xp.service.ImmutableAppKeyList;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.cloud.operator.helpers.InformerEventHandler;
import com.enonic.cloud.operator.helpers.Xp7DeploymentInfo;

import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.common.Utils.hasMetadataComparator;

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
        // If url has changed
        if ( !oldResource.getXp7AppSpec().getUrl().equals( newResource.getXp7AppSpec().getUrl() ) )
        {
            // Try to reinstall app
            if ( !installApp( newResource ) )
            {
                // If failed, remove app info and retry later
                updateAppInfo( newResource, null );
            }
        }

        // If app marked for deletion and has finalizers
        List<String> finalizers = newResource.getMetadata().getFinalizers();
        if ( newResource.getMetadata().getDeletionTimestamp() != null &&
            finalizers.contains( cfgStr( "operator.finalizer.app.uninstall" ) ) )
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
        // Collect lists
        List<Xp7App> appsToInstall = searchers.xp7App().query().
            hasNotBeenDeleted().
            hasFinalizer( cfgStr( "operator.finalizer.app.uninstall" ) ).
            filter( this::appInfoIsNull ).
            sorted( hasMetadataComparator() ).
            list();
        List<Xp7App> appsToUninstall = searchers.xp7App().query().
            hasBeenDeleted().
            hasFinalizer( cfgStr( "operator.finalizer.app.uninstall" ) ).
            sorted( hasMetadataComparator() ).
            list();

        // Handle lists
        appsToInstall.forEach( this::installApp );
        appsToUninstall.forEach( this::uninstallApp );
    }

    private synchronized boolean installApp( final Xp7App app )
    {
        // Check if XP is running
        if ( !xp7DeploymentInfo.xpRunning( app.getMetadata().getNamespace() ) )
        {
            return false;
        }

        // Try to install
        String failure = null;
        AppInfo appInfo = null;
        try
        {
            AppInstallResponse response =
                xpClientCache.install( app.getMetadata().getNamespace() ).apply( ImmutableAppInstallRequest.builder().
                    url( app.getXp7AppSpec().getUrl() ).
                    build() );
            if ( response.failure() != null )
            {
                failure = response.failure();
            }
            else
            {
                appInfo = response.applicationInstalledJson().application();
            }
        }
        catch ( Exception e )
        {
            failure = e.getMessage();
        }

        // On Failure
        if ( failure != null || appInfo == null )
        {
            log.warn( "Failed installing app: " + failure );
            return false;
        }

        updateAppInfo( app, appInfo );
        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean uninstallApp( final Xp7App app )
    {
        // If whole namespace is being deleted just remove the app finalizer
        if ( namespaceBeingTerminated( app ) )
        {
            return removeFinalizer( app );
        }

        // If there is no app info, the app is not installed, hence just remove the app finalizer
        if ( appInfoIsNull( app ) )
        {
            return removeFinalizer( app );
        }

        // If deployment is being deleted just remove the app finalizer
        if ( xp7DeploymentInfo.xpDeleted( app.getMetadata().getNamespace() ) )
        {
            return removeFinalizer( app );
        }

        // XP is not running, nothing we can dos
        if ( !xp7DeploymentInfo.xpRunning( app.getMetadata().getNamespace() ) )
        {
            return false;
        }

        // Try to uninstall
        try
        {
            xpClientCache.uninstall( app.getMetadata().getNamespace() ).accept( ImmutableAppKeyList.builder().addKey( app.
                getXp7AppStatus().
                getXp7AppStatusFields().
                getXp7AppStatusFieldsAppInfo().
                getKey() ).
                build() );
            return removeFinalizer( app );
        }
        catch ( Exception e )
        {
            log.warn( "Failed uninstalling app: " + e.getMessage() );
            return false;
        }
    }

    private void updateAppInfo( Xp7App resource, AppInfo appInfo )
    {
        Xp7AppStatus status;
        if ( appInfo == null )
        {
            status = new Xp7AppStatus().
                withState( Xp7AppStatus.State.PENDING ).
                withMessage( "Pending install" ).
                withXp7AppStatusFields( null );
        }
        else
        {
            status = new Xp7AppStatus().
                withState( Xp7AppStatus.State.PENDING ).
                withMessage( "Installed" ).
                withXp7AppStatusFields( null );
        }

        K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            edit().
            withStatus( status ) );
    }

    private boolean removeFinalizer( Xp7App resource )
    {
        List<String> finalizers = resource.getMetadata().getFinalizers();

        if ( finalizers.remove( cfgStr( "operator.finalizer.app.uninstall" ) ) )
        {
            K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
                inNamespace( resource.getMetadata().getNamespace() ).
                withName( resource.getMetadata().getName() ).
                edit().withNewMetadataLike( resource.getMetadata() ).withFinalizers( finalizers ).endMetadata() );
            return true;
        }
        return false;
    }

    private boolean appInfoIsNull( Xp7App app )
    {
        return app.getXp7AppStatus() == null || app.getXp7AppStatus().getXp7AppStatusFields() == null ||
            app.getXp7AppStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo() == null;
    }

    private boolean namespaceBeingTerminated( Xp7App app )
    {
        return clients.k8s().namespaces().withName( app.getMetadata().getNamespace() ).get().getMetadata().getDeletionTimestamp() != null;
    }
}
