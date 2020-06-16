package com.enonic.cloud.operator.v1alpha1xp7app;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatusFields;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatus;
import com.enonic.cloud.operator.InformerEventHandler;

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

    void onStartup( @Observes StartupEvent _ev )
    {
        cfgIfBool( "operator.status.enabled", () -> {
            listenToInformer( xp7AppSharedIndexInformer );
            taskRunner.scheduleAtFixedRate( this, cfgLong( "operator.tasks.initialDelayMs" ),
                                            cfgLong( "operator.tasks.app.install.periodMs" ), TimeUnit.MILLISECONDS );
        } );
    }

    @Override
    public void onAdd( final Xp7App newResource )
    {
        // Add finalizer
        // TODO: Move this to MutationApi
        K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
            inNamespace( newResource.getMetadata().getNamespace() ).
            withName( newResource.getMetadata().getName() ).
            edit().withNewMetadataLike( newResource.getMetadata() ).
            withFinalizers( Collections.singletonList( cfgStr( "operator.finalizer.app.uninstall" ) ) ).endMetadata() );
    }

    @Override
    public void onUpdate( final Xp7App oldResource, final Xp7App newResource )
    {
        // If url has changed or bootstrap was changed
        if ( !oldResource.getXp7AppSpec().getUrl().equals( newResource.getXp7AppSpec().getUrl() ) )
        {
            // Remove app key
            K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
                inNamespace( newResource.getMetadata().getNamespace() ).
                withName( newResource.getMetadata().getName() ).
                edit().
                withStatus( new Xp7AppStatus().
                    withMessage( "Pending install" ).
                    withState( Xp7AppStatus.State.PENDING ).
                    withXp7AppStatusFields( new Xp7AppStatusFields() ) ) );
        }
    }

    @Override
    public void onDelete( final Xp7App oldResource, final boolean b )
    {
        // Do nothing
    }

    @Override
    public void run()
    {
        xp7AppInformerSearcher.getStream().
            filter( app -> app.getXp7AppStatus() != null ).
            filter( app -> app.getMetadata().getDeletionTimestamp() == null ).
            filter( app -> app.getXp7AppStatus().getXp7AppStatusFields().getAppKey() == null ).
            forEach( this::installApp );

        xp7AppInformerSearcher.getStream().
            filter( app -> app.getMetadata().getDeletionTimestamp() != null ).
            filter( app -> !app.getMetadata().getFinalizers().isEmpty() ).
            forEach( this::uninstallApp );
    }

    private void installApp( final Xp7App app )
    {
        if ( !xpRunning( app ) )
        {
            return;
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
            // Add app key
            K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
                inNamespace( app.getMetadata().getNamespace() ).
                withName( app.getMetadata().getName() ).
                edit().
                withStatus( new Xp7AppStatus().
                    withState( Xp7AppStatus.State.STOPPED ).
                    withMessage( "Installed" ).
                    withXp7AppStatusFields( new Xp7AppStatusFields().withAppKey( appKey ) ) ) );
        }
        else
        {
            log.warn( "Failed installing app: " + failure );
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void uninstallApp( final Xp7App app )
    {
        if ( app.getXp7AppStatus() == null )
        {
            // Ignore
        }
        else if ( clients.k8s().namespaces().withName( app.getMetadata().getNamespace() ).get().getMetadata().getDeletionTimestamp() !=
            null )
        {
            // Ignore
        }
        else if ( !xpRunning( app ) )
        {
            return;
        }
        else if ( app.getXp7AppStatus().getXp7AppStatusFields().getAppKey() != null )
        {
            try
            {
                xpClientCache.uninstall( app ).accept(
                    ImmutableAppUninstallRequest.builder().addKey( app.getXp7AppStatus().getXp7AppStatusFields().getAppKey() ).build() );
            }
            catch ( Exception e )
            {
                log.warn( "Failed uninstalling app: " + e.getMessage() );
                return;
            }
        }

        // Remove finalizer
        K8sLogHelper.logDoneable( clients.xp7Apps().crdClient().
            inNamespace( app.getMetadata().getNamespace() ).
            withName( app.getMetadata().getName() ).
            edit().withNewMetadataLike( app.getMetadata() ).withFinalizers( Collections.emptyList() ).endMetadata() );
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
