package com.enonic.kubernetes.operator.v1alpha1xp7app;

import com.enonic.kubernetes.apis.xp.XpClientCache;
import com.enonic.kubernetes.apis.xp.XpClientException;
import com.enonic.kubernetes.apis.xp.service.AppEvent;
import com.enonic.kubernetes.apis.xp.service.AppInfo;
import com.enonic.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppStatusFields;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppStatusFieldsAppInfo;
import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.kubernetes.Predicates.inNamespace;
import static com.enonic.kubernetes.operator.v1alpha1xp7app.Predicates.notSuccessfullyInstalled;
import static com.enonic.kubernetes.operator.v1alpha1xp7app.Predicates.successfullyInstalled;
import static com.enonic.kubernetes.operator.v1alpha2xp7deployment.Predicates.running;

@Singleton
public class HandlerStatus
{
    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    XpClientCache xpClientCache;

    public synchronized boolean updateStatus( final Xp7Deployment deployment, final Xp7App app )
    {
        Optional<AppInfo> appInfo = Optional.empty();

        if (successfullyInstalled().test( app ) && running().test( deployment )) {
            try {
                appInfo = xpClientCache.appInfo( app.getMetadata().getNamespace(), app.getStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo().getKey() );
            } catch (XpClientException e) {
                return updateStatus( Optional.of( app ), findDeployment( app.getMetadata().getNamespace() ), appInfo, Optional.of( "Unable to connect to XP" ) );
            }
        }

        return updateStatus( Optional.of( app ), findDeployment( app.getMetadata().getNamespace() ), appInfo, Optional.empty() );
    }

    public synchronized boolean updateStatus( final Xp7Deployment deployment )
        throws IOException
    {
        return searchers.xp7App()
            .filter( inNamespace( deployment.getMetadata().getNamespace() ) )
            .map( app -> updateStatus( deployment, app ) )
            .collect( Collectors.toSet() )
            .contains( true );
    }

    public synchronized boolean updateStatus( final Xp7App app, final AppEvent appEvent )
    {
        return updateStatus( Optional.of( app ), findDeployment( appEvent.namespace() ), Optional.of( appEvent.info() ), Optional.empty() );
    }

    public synchronized boolean updateStatus( final Xp7App app, final AppInfo appInfo )
    {
        return updateStatus( Optional.of( app ), findDeployment( app.getMetadata().getNamespace() ), Optional.of( appInfo ), Optional.empty() );
    }

    public synchronized boolean updateStatus( final Xp7App app, final String error )
    {
        return updateStatus( Optional.of( app ), findDeployment( app.getMetadata().getNamespace() ), Optional.empty(), Optional.of( error ) );
    }

    private Optional<Xp7Deployment> findDeployment( String namespace )
    {
        return searchers.xp7Deployment().find( inNamespace( namespace ) );
    }

    private boolean updateStatus( final Optional<Xp7App> optionalApp, final Optional<Xp7Deployment> optionalDeployment, final Optional<AppInfo> optionalAppInfo, final Optional<String> error )
    {
        if (optionalApp.isEmpty()) {
            // This can happen if apps are installed in XP but not with the operator
            return false;
        }
        Xp7App app = optionalApp.get();

        if (error.isPresent()) {
            return setStatus( app, Xp7AppStatus.State.ERROR, error.get(), optionalAppInfo );
        }

        if (optionalDeployment.isEmpty()) {
            return setStatus( app, Xp7AppStatus.State.ERROR, "XP deployment not found", optionalAppInfo );
        }
        Xp7Deployment deployment = optionalDeployment.get();

        // Deployment is stopped
        if (!deployment.getSpec().getEnabled()) {
            return setStatus( app, Xp7AppStatus.State.STOPPED, "XP deployment stopped", optionalAppInfo );
        }

        // App is not installed, there is no app info, and we have an error
        // lets not change that until we have some app info
        if (notSuccessfullyInstalled().test( app ) &&
            optionalAppInfo.isEmpty() &&
            app.getStatus().getState() == Xp7AppStatus.State.ERROR) {
            return false;
        }

        // No app info
        if (optionalAppInfo.isEmpty()) {
            return setStatus( app, Xp7AppStatus.State.PENDING, "Waiting for XP to start", optionalAppInfo );
        }

        AppInfo appInfo = optionalAppInfo.get();
        switch (appInfo.state()) {
            case "started":
                if (app.getSpec().getEnabled()) {
                    return setStatus( app, Xp7AppStatus.State.RUNNING, "OK", optionalAppInfo );
                } else {
                    return setStatus( app, Xp7AppStatus.State.PENDING, "Has not been stopped", optionalAppInfo );
                }
            case "stopped":
                if (app.getSpec().getEnabled()) {
                    return setStatus( app, Xp7AppStatus.State.PENDING, "Has not been started", optionalAppInfo );
                } else {
                    return setStatus( app, Xp7AppStatus.State.STOPPED, "OK", optionalAppInfo );
                }

            default:
                return setStatus( app, Xp7AppStatus.State.ERROR, String.format( "Invalid app state '%s'", appInfo.state() ), optionalAppInfo );
        }
    }

    protected boolean setStatus( final Xp7App app, final Xp7AppStatus.State state, final String message, final Optional<AppInfo> appInfo )
    {
        final Xp7AppStatus newStatus = new Xp7AppStatus()
            .withState( state )
            .withMessage( message )
            .withXp7AppStatusFields( appInfo.map( i -> new Xp7AppStatusFields()
                .withXp7AppStatusFieldsAppInfo( new Xp7AppStatusFieldsAppInfo()
                    .withDescription( i.description() )
                    .withDisplayName( i.displayName() )
                    .withKey( i.key() )
                    .withModifiedTime( i.modifiedTime() )
                    .withState( i.state() )
                    .withUrl( i.url() )
                    .withVendorName( i.vendorName() )
                    .withVendorUrl( i.vendorUrl() )
                    .withVersion( i.version() ) ) )
                .orElse( app.getStatus().getXp7AppStatusFields() ) );

        if (!newStatus.equals( app.getStatus() )) {
            K8sLogHelper.logEdit( clients.xp7Apps().
                inNamespace( app.getMetadata().getNamespace() ).
                withName( app.getMetadata().getName() ), a -> {
                a.setStatus( newStatus );
                return a;
            } );
            return true;
        }

        return false;
    }
}
