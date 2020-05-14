package com.enonic.cloud.operator.v1alpha1xp7app;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.AppInstallResponse;
import com.enonic.cloud.apis.xp.service.ImmutableAppInstallRequest;
import com.enonic.cloud.apis.xp.service.ImmutableAppUninstallRequest;
import com.enonic.cloud.common.staller.TaskRunner;
import com.enonic.cloud.kubernetes.caches.NamespaceCache;
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
import static com.enonic.cloud.common.Configuration.cfgLong;
import static com.enonic.cloud.common.Configuration.cfgStr;

@ApplicationScoped
public class OperatorAppInstaller
    implements ResourceEventHandler<V1alpha1Xp7App>, Runnable
{
    private static final Logger log = LoggerFactory.getLogger( OperatorAppInstaller.class );

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CrdClient crdClient;

    @Inject
    V1alpha1Xp7AppCache v1alpha1Xp7AppCache;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    V1alpha2Xp7DeploymentCache v1alpha2Xp7DeploymentCache;

    @Inject
    NamespaceCache namespaceCache;

    @Inject
    XpClientCache xpClientCache;

    @Inject
    TaskRunner taskRunner;

    void onStartup( @Observes StartupEvent _ev )
    {
        cfgIfBool( "operator.status.enabled", () -> {
            v1alpha1Xp7AppCache.addEventListener( this );
            taskRunner.scheduleAtFixedRate( this, cfgLong( "operator.tasks.initialDelayMs" ), cfgLong( "operator.tasks.app.install.periodMs" ), TimeUnit.MILLISECONDS );
        } );
    }

    @Override
    public void onAdd( final V1alpha1Xp7App newResource )
    {
        // Add finalizer
        // TODO: Move this to MutationApi
        K8sLogHelper.logDoneable( crdClient.xp7Apps().
            inNamespace( newResource.getMetadata().getNamespace() ).
            withName( newResource.getMetadata().getName() ).
            edit().
            withFinalizers( Collections.singletonList( cfgStr( "operator.finalizer.app.uninstall" ) ) ) );
    }

    @Override
    public void onUpdate( final V1alpha1Xp7App oldResource, final V1alpha1Xp7App newResource )
    {
        // If url has changed or bootstrap was changed
        if ( !oldResource.getSpec().url().equals( newResource.getSpec().url() ) ||
            !oldResource.getSpec().options().bootstrap().equals( newResource.getSpec().options().bootstrap() ) )
        {
            // Remove app key
            K8sLogHelper.logDoneable( crdClient.xp7Apps().
                inNamespace( newResource.getMetadata().getNamespace() ).
                withName( newResource.getMetadata().getName() ).
                edit().
                withStatus( getBuilder( newResource.getStatus() ).
                    fields( ImmutableV1alpha1Xp7AppStatusFields.builder().build() ).
                    build() ) );
        }
    }

    @Override
    public void onDelete( final V1alpha1Xp7App oldResource, final boolean b )
    {
        // Do nothing
    }

    @Override
    public void run()
    {
        v1alpha1Xp7AppCache.getStream().
            filter( app -> app.getStatus() != null ).
            filter( app -> !app.getSpec().options().bootstrap() ).
            filter( app -> app.getMetadata().getDeletionTimestamp() == null ).
            filter( app -> app.getStatus().fields().appKey() == null ).
            forEach( this::installApp );

        v1alpha1Xp7AppCache.getStream().
            filter( app -> app.getMetadata().getDeletionTimestamp() != null ).
            filter( app -> !app.getMetadata().getFinalizers().isEmpty() ).
            forEach( this::uninstallApp );
    }

    private void installApp( final V1alpha1Xp7App app )
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
                url( app.getSpec().url() ).
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
            K8sLogHelper.logDoneable( crdClient.xp7Apps().
                inNamespace( app.getMetadata().getNamespace() ).
                withName( app.getMetadata().getName() ).
                edit().
                withStatus( getBuilder( app.getStatus() ).
                    fields( ImmutableV1alpha1Xp7AppStatusFields.builder().appKey( appKey ).build() ).
                    build() ) );
        }
        else
        {
            log.warn( "Failed installing app: " + failure );
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void uninstallApp( final V1alpha1Xp7App app )
    {
        if ( app.getStatus() == null )
        {
            // Ignore
        }
        else if ( namespaceCache.get( null, app.getMetadata().getNamespace() ).get().getMetadata().getDeletionTimestamp() != null )
        {
            // Ignore
        }
        else if ( !xpRunning( app ) )
        {
            return;
        }
        else if ( app.getStatus().fields().appKey() != null )
        {
            try
            {
                xpClientCache.uninstall( app ).accept(
                    ImmutableAppUninstallRequest.builder().addKey( app.getStatus().fields().appKey() ).build() );
            }
            catch ( Exception e )
            {
                log.warn( "Failed uninstalling app: " + e.getMessage() );
                return;
            }
        }

        // Remove finalizer
        K8sLogHelper.logDoneable( crdClient.xp7Apps().
            inNamespace( app.getMetadata().getNamespace() ).
            withName( app.getMetadata().getName() ).
            edit().
            withFinalizers( Collections.emptyList() ) );
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
