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

/**
 * This operator class updates Xp7App status fields
 */
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
        Xp7AppStatus currentStatus = resource.getXp7AppStatus() != null ? resource.getXp7AppStatus() : new Xp7AppStatus().
            withState( Xp7AppStatus.State.PENDING ).
            withMessage( "Pending install" );

        // If there is no app info
        if ( currentStatus.getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo() == null )
        {
            return currentStatus;
        }

        // XP is not running
        if ( !xp7DeploymentInfo.xpRunning( resource.getMetadata().getNamespace() ) )
        {
            return currentStatus.
                withState( Xp7AppStatus.State.PENDING ).
                withMessage( "Xp7Deployment not in RUNNING state" );
        }

        List<AppInfo> infoList = getAppInfoList( resource );

        // Failed calling XP
        if ( infoList == null )
        {
            return currentStatus.
                withState( Xp7AppStatus.State.ERROR ).
                withMessage( "Failed calling XP" );
        }

        AppInfo appInfo = getAppState( resource, infoList );

        // App not found in XP
        if ( appInfo == null )
        {
            return currentStatus.
                withState( Xp7AppStatus.State.ERROR ).
                withMessage( "App not found in XP" );
        }

        // Update state/message
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

    private List<AppInfo> getAppInfoList( final Xp7App resource )
    {
        String mapKey = resource.getMetadata().getNamespace();
        if ( !appInfoMap.containsKey( mapKey ) )
        {
            List<AppInfo> appInfo = null;
            try
            {
                appInfo = xpClientCache.list( resource.getMetadata().getNamespace() ).
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

        return appInfoMap.get( mapKey );
    }

    private AppInfo getAppState( final Xp7App resource, final List<AppInfo> infoList )
    {
        for ( AppInfo appInfo : infoList )
        {
            if ( appInfo.key().equals( resource.
                getXp7AppStatus().
                getXp7AppStatusFields().
                getXp7AppStatusFieldsAppInfo().
                getKey() ) )
            {
                return appInfo;
            }
        }
        return null;
    }
}
