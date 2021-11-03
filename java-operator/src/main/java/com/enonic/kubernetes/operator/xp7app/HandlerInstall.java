package com.enonic.kubernetes.operator.xp7app;

import com.enonic.kubernetes.apis.xp.XpClientCache;
import com.enonic.kubernetes.apis.xp.service.AppInfo;
import com.enonic.kubernetes.client.v1.xp7app.Xp7App;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;
import com.enonic.kubernetes.common.TaskRunner;
import com.enonic.kubernetes.kubernetes.ActionLimiter;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Predicates.*;
import static com.enonic.kubernetes.operator.xp7app.Predicates.notSuccessfullyInstalled;
import static com.enonic.kubernetes.operator.xp7deployment.Predicates.parent;
import static com.enonic.kubernetes.operator.xp7deployment.Predicates.running;

@Singleton
public class HandlerInstall
{
    private static final Logger log = LoggerFactory.getLogger( HandlerInstall.class );

    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    HandlerStatus handlerStatus;

    @Inject
    XpClientCache xpClientCache;

    @Inject
    TaskRunner taskRunner;

    ActionLimiter limiter;

    void onStart( @Observes StartupEvent ev )
    {
        limiter = new ActionLimiter( this.getClass().getSimpleName(), taskRunner, 1000L, 15000L );
    }

    public void installApp( final Xp7App app )
    {
        // This is never going to work, do not even try
        if (app.getSpec().getUrl().startsWith( "http://localhost" )) {
            handlerStatus.updateStatus( app, "Cannot install, app URL invalid" );
            return;
        }

        Optional<Xp7Deployment> xp7Deployment = searchers.xp7Deployment().find( parent( app ), running(), isNotDeleted() );

        // XP is not running, nothing we can do
        if (xp7Deployment.isEmpty()) {
            return;
        }

        limiter.limit( app, xp7App -> doInstallApp(xp7Deployment.get(), xp7App) );
    }

    public void uninstallApp( final Xp7App app )
    {
        // If whole namespace is being deleted just remove the app finalizer
        if (searchers.namespace().match( contains( app ), isDeleted() )) {
            removeFinalizer( app );
            return;
        }

        // If the app is not installed, just remove the app finalizer
        if (notSuccessfullyInstalled().test( app )) {
            removeFinalizer( app );
            return;
        }

        // If deployment is being deleted just remove the app finalizer
        if (!searchers.xp7Deployment().match( parent( app ), isNotDeleted() )) {
            removeFinalizer( app );
            return;
        }

        Optional<Xp7Deployment> xp7Deployment = searchers.xp7Deployment().find( parent( app ), running() );

        // XP is not running, nothing we can do
        if (xp7Deployment.isEmpty()) {
            return;
        }

        limiter.limit( app, xp7App -> doUninstallApp(xp7Deployment.get(), xp7App));
    }


    private void doInstallApp( final Xp7Deployment deployment, final Xp7App app )
    {
        // Try to install
        try {
            AppInfo appInfo = xpClientCache.
                appInstall( app.getMetadata().getNamespace(), deployment.getMetadata().getName(), app.getSpec().getUrl(), app.getSpec().getSha512() );

            handlerStatus.updateStatus( app, appInfo );
        } catch (Exception e) {
            log.warn( String.format( "Failed installing app %s in NS %s: %s", app.getMetadata().getName(), app.getMetadata().getNamespace(),
                e.getMessage() ) );

            handlerStatus.updateStatus( app, "Cannot install, see operator logs" );
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean doUninstallApp( final Xp7Deployment deployment, final Xp7App app )
    {
        // Try to uninstall
        try {
            xpClientCache.appUninstall( deployment.getMetadata().getNamespace(), deployment.getMetadata().getName(), app.getStatus()
                .getXp7AppStatusFields()
                .getXp7AppStatusFieldsAppInfo()
                .getKey() );
            return removeFinalizer( app );
        } catch (Exception e) {
            log.warn( String.format(
                "Failed uninstalling app %s in NS %s: %s",
                app.getMetadata().getName(),
                app.getMetadata().getNamespace(),
                e.getMessage() ) );
            handlerStatus.updateStatus( app, "Cannot uninstall, see operator logs" );
            return false;
        }
    }

    public void triggerAppReinstall( final Xp7App app )
    {
        K8sLogHelper.logEdit( clients.xp7Apps()
            .inNamespace( app.getMetadata().getNamespace() )
            .withName( app.getMetadata().getName() ), a -> {
            // We do this by resetting the whole status object
            a.setStatus( null );
            return a;
        } );
    }

    private boolean removeFinalizer( Xp7App resource )
    {
        List<String> finalizers = resource.getMetadata().getFinalizers();

        if (finalizers.remove( cfgStr( "operator.charts.values.finalizers.app.uninstall" ) )) {
            K8sLogHelper.logEdit( clients.xp7Apps()
                .inNamespace( resource.getMetadata().getNamespace() )
                .withName( resource.getMetadata().getName() ), a -> {
                a.getMetadata().setFinalizers( Collections.emptyList() );
                return a;
            } );
            return true;
        }
        return false;
    }
}
