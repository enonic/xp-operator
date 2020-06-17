package com.enonic.cloud.operator.v1alpha1xp7app;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.AppInstallResponse;
import com.enonic.cloud.apis.xp.service.ImmutableAppInstallRequest;
import com.enonic.cloud.apis.xp.service.ImmutableAppUninstallRequest;
import com.enonic.cloud.common.staller.TaskRunner;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.operator.functions.HasMetadataOlderThanImpl;
import com.enonic.cloud.operator.functions.XpDeletedImpl;
import com.enonic.cloud.operator.functions.XpRunningImpl;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgIfBool;
import static com.enonic.cloud.common.Configuration.cfgLong;
import static com.enonic.cloud.common.Configuration.cfgStr;

@ApplicationScoped
public class OperatorAppInstaller
    extends InformerEventHandler<Xp7App>
    implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger( OperatorAppInstaller.class );

    @Inject
    Clients clients;

    @Inject
    SharedIndexInformer<Xp7App> xp7AppSharedIndexInformer;

    @Inject
    InformerSearcher<Xp7App> xp7AppInformerSearcher;

    @Inject
    InformerSearcher<Xp7Deployment> xp7DeploymentInformerSearcher;

    @Inject
    XpClientCache xpClientCache;

    @Inject
    TaskRunner taskRunner;

    Predicate<HasMetadata> xpDeleted;

    Predicate<HasMetadata> xpRunning;

    Predicate<HasMetadata> olderThanFiveSeconds;

    void onStartup( @Observes StartupEvent _ev )
    {
        xpDeleted = XpDeletedImpl.of( xp7DeploymentInformerSearcher );
        xpRunning = XpRunningImpl.of( xp7DeploymentInformerSearcher );
        olderThanFiveSeconds = HasMetadataOlderThanImpl.of( 5L );
        cfgIfBool( "operator.status.enabled", () -> {
            listenToInformer( xp7AppSharedIndexInformer );
            taskRunner.scheduleAtFixedRate( this, cfgLong( "operator.tasks.initialDelayMs" ),
                                            cfgLong( "operator.tasks.app.install.periodMs" ), TimeUnit.MILLISECONDS );
        } );
    }

    @Override
    public void onAdd( final Xp7App newResource )
    {
        if ( !olderThanFiveSeconds.test( newResource ) )
        {
            installApp( newResource );
        }
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
    }

    @Override
    public void onDelete( final Xp7App oldResource, final boolean b )
    {
        // Handled by finalizers
    }

    @Override
    public void run()
    {
        xp7AppInformerSearcher.getStream().
            filter( app -> app.getMetadata().getDeletionTimestamp() == null ).
            filter( app -> !app.getMetadata().getFinalizers().isEmpty() ).
            filter( this::appKeyIsNull ).
            forEach( this::installApp );

        xp7AppInformerSearcher.getStream().
            filter( app -> app.getMetadata().getDeletionTimestamp() != null ).
            filter( app -> !app.getMetadata().getFinalizers().isEmpty() ).
            forEach( this::uninstallApp );
    }

    private synchronized boolean installApp( final Xp7App app )
    {
        if ( !xpRunning.test( app ) )
        {
            return false;
        }

        String failure = null;
        String appKey = null;
        try
        {
            AppInstallResponse response = xpClientCache.install( app ).apply( ImmutableAppInstallRequest.builder().
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

    private synchronized boolean uninstallApp( final Xp7App app )
    {
        if ( namespaceBeingTerminated( app ) )
        {
            return removeFinalizer( app );
        }

        if ( appKeyIsNull( app ) )
        {
            return removeFinalizer( app );
        }

        if ( xpDeleted.test( app ) )
        {
            return removeFinalizer( app );
        }

        if ( xpRunning.test( app ) )
        {
            try
            {
                xpClientCache.uninstall( app ).accept(
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
