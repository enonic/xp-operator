package com.enonic.cloud.operator.v1alpha1xp7app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.operator.functions.XpRunningImpl;
import com.enonic.cloud.operator.helpers.StatusHandler;

@ApplicationScoped
public class OperatorAppStatus
    extends StatusHandler<Xp7App, Xp7AppStatus>
{
    private static final Logger log = LoggerFactory.getLogger( OperatorAppStatus.class );

    @Inject
    Clients clients;

    @Inject
    InformerSearcher<Xp7App> xp7AppInformerSearcher;

    @Inject
    InformerSearcher<Xp7Deployment> xp7DeploymentInformerSearcher;

    @Inject
    XpClientCache xpClientCache;

    Map<String, List<AppInfo>> appInfoMap;

    Predicate<HasMetadata> xpRunning;

    @Override
    public void onStartup( @Observes final StartupEvent _ev )
    {
        xpRunning = XpRunningImpl.of( xp7DeploymentInformerSearcher );
        super.onStartup( _ev );
    }

    @Override
    protected void preRun()
    {
        appInfoMap = new HashMap<>();
    }

    @Override
    protected InformerSearcher<Xp7App> informerSearcher()
    {
        return xp7AppInformerSearcher;
    }

    @Override
    protected Xp7AppStatus getStatus( final Xp7App resource )
    {
        return resource.getXp7AppStatus();
    }

    @Override
    protected Doneable<Xp7App> updateStatus( final Xp7App resource, final Xp7AppStatus newStatus )
    {
        return clients.xp7Apps().crdClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            edit().
            withStatus( newStatus );
    }

    @Override
    protected Xp7AppStatus pollStatus( final Xp7App resource )
    {
        Xp7AppStatus currentStatus = resource.getXp7AppStatus();

        if ( currentStatus.getXp7AppStatusFields().getAppKey() == null )
        {
            return currentStatus.
                withState( Xp7AppStatus.State.PENDING ).
                withMessage( "Pending install" );
        }

        return checkXp( resource, currentStatus );
    }

    private Xp7AppStatus checkXp( final Xp7App resource, final Xp7AppStatus currentStatus )
    {
        if ( !xpRunning.test( resource ) )
        {
            return currentStatus.
                withState( Xp7AppStatus.State.PENDING ).
                withMessage( "Xp7Deployment not in RUNNING state" );
        }

        String mapKey = resource.getMetadata().getNamespace();
        if ( !appInfoMap.containsKey( mapKey ) )
        {
            List<AppInfo> appInfo = null;
            try
            {
                appInfo = xpClientCache.list( resource ).
                    get().
                    applications();
            }
            catch ( Exception e )
            {
                log.warn( String.format( "Failed calling XP for app '%s' in NS '%s': %s", resource.getMetadata().getName(),
                                         resource.getMetadata().getNamespace(), e.getMessage() ) );
            }
            appInfoMap.put( mapKey, appInfo );
        }

        List<AppInfo> info = appInfoMap.get( mapKey );

        if ( info == null )
        {
            return currentStatus.
                withState( Xp7AppStatus.State.ERROR ).
                withMessage( "Failed calling XP" );
        }

        for ( AppInfo appInfo : info )
        {
            if ( appInfo.key().equals( currentStatus.getXp7AppStatusFields().getAppKey() ) )
            {
                switch ( appInfo.state() )
                {
                    case "started":
                        return currentStatus.
                            withState( Xp7AppStatus.State.RUNNING ).
                            withMessage( "Started" );
                    case "stopped":
                        return currentStatus.
                            withState( Xp7AppStatus.State.STOPPED ).
                            withMessage( "Stopped" );
                    default:
                        return currentStatus.
                            withState( Xp7AppStatus.State.ERROR ).
                            withMessage( "Invalid app state" );
                }
            }
        }

        return currentStatus.
            withState( Xp7AppStatus.State.ERROR ).
            withMessage( "App not found in XP" );
    }
}
