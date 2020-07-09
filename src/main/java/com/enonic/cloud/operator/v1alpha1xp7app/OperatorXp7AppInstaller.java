package com.enonic.cloud.operator.v1alpha1xp7app;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.AppInstallResponse;
import com.enonic.cloud.apis.xp.service.ImmutableAppInstallRequest;
import com.enonic.cloud.apis.xp.service.ImmutableAppUninstallRequest;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.operator.helpers.InformerEventHandler;
import com.enonic.cloud.operator.helpers.Xp7DeploymentInfo;

import static com.enonic.cloud.common.Configuration.cfgStr;


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
    protected void init()
    {
        // Do nothing
    }

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
                // If failed, remove app key and retry later
                updateAppKey( newResource, null );
            }
        }

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
        searchers.xp7App().query().
            hasNotBeenDeleted().
            hasFinalizer( cfgStr( "operator.finalizer.app.uninstall" ) ).
            filter( this::appKeyIsNull ).
            forEach( this::installApp );

        searchers.xp7App().query().
            hasBeenDeleted().
            hasFinalizer( cfgStr( "operator.finalizer.app.uninstall" ) ).
            forEach( this::uninstallApp );
    }

    private synchronized boolean installApp( final Xp7App app )
    {
        if ( !xp7DeploymentInfo.xpRunning( app.getMetadata().getNamespace() ) )
        {
            return false;
        }

        String failure = null;
        String appKey = null;
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
                appKey = response.applicationInstalledJson().application().key();
            }
        }
        catch ( Exception e )
        {
            failure = e.getMessage();
        }

        if ( failure == null && appKey != null )
        {
            updateAppKey( app, appKey );
            return true;
        }
        else
        {
            log.warn( "Failed installing app: " + failure );
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean uninstallApp( final Xp7App app )
    {
        if ( namespaceBeingTerminated( app ) )
        {
            return removeFinalizer( app );
        }

        if ( appKeyIsNull( app ) )
        {
            return removeFinalizer( app );
        }

        if ( xp7DeploymentInfo.xpDeleted( app.getMetadata().getNamespace() ) )
        {
            return removeFinalizer( app );
        }

        if ( xp7DeploymentInfo.xpRunning( app.getMetadata().getNamespace() ) )
        {
            try
            {
                xpClientCache.uninstall( app.getMetadata().getNamespace() ).accept(
                    ImmutableAppUninstallRequest.builder().addKey( app.getXp7AppStatus().getXp7AppStatusFields().getAppKey() ).build() );
                return removeFinalizer( app );
            }
            catch ( Exception e )
            {
                log.warn( "Failed uninstalling app: " + e.getMessage() );
            }
        }

        return false;
    }

    private void updateAppKey( Xp7App resource, String appKey )
    {
        K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            edit().
            withStatus( resource.getXp7AppStatus().
                withXp7AppStatusFields( resource.getXp7AppStatus().getXp7AppStatusFields().
                    withAppKey( appKey ) ) ) );
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

    private boolean appKeyIsNull( Xp7App app )
    {
        return app.getXp7AppStatus() == null || app.getXp7AppStatus().getXp7AppStatusFields() == null ||
            app.getXp7AppStatus().getXp7AppStatusFields().getAppKey() == null;
    }

    private boolean namespaceBeingTerminated( Xp7App app )
    {
        return clients.k8s().namespaces().withName( app.getMetadata().getNamespace() ).get().getMetadata().getDeletionTimestamp() != null;
    }
}
