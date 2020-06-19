package com.enonic.cloud.operator.v1alpha1xp7app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Doneable;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.AppInfo;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7AppStatus;
import com.enonic.cloud.operator.helpers.HandlerStatus;
import com.enonic.cloud.operator.helpers.Xp7DeploymentInfo;

@Singleton
public class OperatorXp7AppStatus
    extends HandlerStatus<Xp7App, Xp7AppStatus>
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7AppStatus.class );

    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    XpClientCache xpClientCache;

    @Inject
    Xp7DeploymentInfo xp7DeploymentInfo;

    private Map<String, List<AppInfo>> appInfoMap;

    @Override
    protected void preRun()
    {
        appInfoMap = new HashMap<>();
    }

    @Override
    protected InformerSearcher<Xp7App> informerSearcher()
    {
        return searchers.xp7App();
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

    private Xp7AppStatus checkXp( final Xp7App app, final Xp7AppStatus currentStatus )
    {
        if ( !xp7DeploymentInfo.xpRunning( app.getMetadata().getNamespace() ) )
        {
            return currentStatus.
                withState( Xp7AppStatus.State.PENDING ).
                withMessage( "Xp7Deployment not in RUNNING state" );
        }

        String mapKey = app.getMetadata().getNamespace();
        if ( !appInfoMap.containsKey( mapKey ) )
        {
            List<AppInfo> appInfo = null;
            try
            {
                appInfo = xpClientCache.list( app.getMetadata().getNamespace() ).
                    get().
                    applications();
            }
            catch ( Exception e )
            {
                log.warn( String.format( "Failed calling XP for app '%s' in NS '%s': %s", app.getMetadata().getName(),
                                         app.getMetadata().getNamespace(), e.getMessage() ) );
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
