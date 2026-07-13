package com.enonic.kubernetes.operator.xp7app;

import com.enonic.kubernetes.apis.xp.XpClientCache;
import com.enonic.kubernetes.apis.xp.XpClientException;
import com.enonic.kubernetes.apis.xp.service.AppEvent;
import com.enonic.kubernetes.apis.xp.service.AppInfo;
import com.enonic.kubernetes.crd.v1.Xp7App;
import com.enonic.kubernetes.crd.v1.Xp7AppStatus;
import com.enonic.kubernetes.crd.v1.xp7appstatus.Fields;
import com.enonic.kubernetes.crd.v1.Xp7Deployment;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.kubernetes.Predicates.inNamespace;
import static com.enonic.kubernetes.operator.xp7app.Predicates.notSuccessfullyInstalled;
import static com.enonic.kubernetes.operator.xp7app.Predicates.successfullyInstalled;
import static com.enonic.kubernetes.operator.xp7deployment.Predicates.running;

@Singleton
public class HandlerStatus
{
    private static final Logger log = LoggerFactory.getLogger( HandlerStatus.class );

    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    XpClientCache xpClientCache;

    public boolean updateStatus( final Xp7Deployment deployment, final Xp7App app )
    {
        Optional<AppInfo> appInfo = Optional.empty();

        if (successfullyInstalled().test( app ) && running().test( deployment )) {
            try {
                appInfo = xpClientCache.appInfo( deployment.getMetadata().getNamespace(), deployment.getMetadata().getName(), app.getStatus().getFields().getAppInfo().getKey() );
            } catch (XpClientException e) {
                return updateStatus( Optional.of( app ), findDeployment( app.getMetadata().getNamespace() ), appInfo, Optional.of( "Unable to connect to XP" ) );
            }
        }

        return updateStatus( Optional.of( app ), findDeployment( app.getMetadata().getNamespace() ), appInfo, Optional.empty() );
    }

    public boolean updateStatus( final Xp7Deployment deployment )
        throws IOException
    {
        return searchers.xp7App()
            .filter( inNamespace( deployment.getMetadata().getNamespace() ) )
            .map( app -> updateStatus( deployment, app ) )
            .collect( Collectors.toSet() )
            .contains( true );
    }

    public boolean updateStatus( final Xp7App app, final AppEvent appEvent )
    {
        return updateStatus( Optional.of( app ), findDeployment( appEvent.namespace() ), Optional.of( appEvent.info() ), Optional.empty() );
    }

    public boolean updateStatus( final Xp7App app, final AppInfo appInfo )
    {
        return updateStatus( Optional.of( app ), findDeployment( app.getMetadata().getNamespace() ), Optional.of( appInfo ), Optional.empty() );
    }

    public boolean updateStatus( final Xp7App app, final String error )
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
        final Xp7AppStatus newStatus = new Xp7AppStatus();
        newStatus.setState( state );
        newStatus.setMessage( message );
        newStatus.setFields( appInfo.map( i -> {
            com.enonic.kubernetes.crd.v1.xp7appstatus.fields.AppInfo statusAppInfo =
                new com.enonic.kubernetes.crd.v1.xp7appstatus.fields.AppInfo();
            statusAppInfo.setDescription( i.description() );
            statusAppInfo.setDisplayName( i.displayName() );
            statusAppInfo.setKey( i.key() );
            statusAppInfo.setModifiedTime( i.modifiedTime() );
            statusAppInfo.setState( i.state() );
            statusAppInfo.setUrl( i.url() );
            statusAppInfo.setVendorName( i.vendorName() );
            statusAppInfo.setVendorUrl( i.vendorUrl() );
            statusAppInfo.setVersion( i.version() );
            Fields fields = new Fields();
            fields.setAppInfo( statusAppInfo );
            return fields;
        } ).orElse( app.getStatus().getFields() ) );

        if (!Serialization.asJson( newStatus ).equals( Serialization.asJson( app.getStatus() ) )) {
            log.debug("Set App status : {} {} in {}", newStatus.getState(), app.getMetadata().getName(), app.getMetadata().getNamespace() );

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
