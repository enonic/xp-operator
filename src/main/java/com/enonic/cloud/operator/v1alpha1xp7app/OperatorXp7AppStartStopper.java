package com.enonic.cloud.operator.v1alpha1xp7app;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.ImmutableAppKeyList;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.common.Utils.hasMetadataComparator;


/**
 * This operator class starts/stops apps in XP
 */
@Singleton
public class OperatorXp7AppStartStopper
    extends InformerEventHandler<Xp7App>
    implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7AppStartStopper.class );

    @Inject
    Searchers searchers;

    @Inject
    XpClientCache xpClientCache;

    @Override
    public void onNewAdd( final Xp7App newResource )
    {
        // Do nothing
    }

    @Override
    public void onUpdate( final Xp7App oldResource, final Xp7App newResource )
    {
        handle( newResource );
    }

    @Override
    public void onDelete( final Xp7App oldResource, final boolean b )
    {
        // Do nothing
    }

    @Override
    public void run()
    {
        // List all apps
        List<Xp7App> apps = searchers.xp7App().query().
            hasNotBeenDeleted().
            hasFinalizer( cfgStr( "operator.finalizer.app.uninstall" ) ).
            sorted( hasMetadataComparator() ).
            list();

        // Handle apps
        apps.forEach( this::handle );
    }

    private void handle( final Xp7App xp7App )
    {
        // If app info is missing
        if ( xp7App.getXp7AppStatus() == null || xp7App.getXp7AppStatus().getXp7AppStatusFields() == null ||
            xp7App.getXp7AppStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo() == null )
        {
            return;
        }

        // If enabled flag is missing
        if ( xp7App.getXp7AppSpec().getEnabled() == null )
        {
            return;
        }

        // Collect info
        Boolean enabled = xp7App.getXp7AppSpec().getEnabled();
        String state = xp7App.getXp7AppStatus().
            getXp7AppStatusFields().
            getXp7AppStatusFieldsAppInfo().
            getState();
        String key = xp7App.getXp7AppStatus().
            getXp7AppStatusFields().
            getXp7AppStatusFieldsAppInfo().
            getKey();
        String namespace = xp7App.getMetadata().getNamespace();

        // Try to start
        if ( enabled && !state.equals( "started" ) )
        {
            try
            {
                xpClientCache.start( namespace ).accept( ImmutableAppKeyList.builder().addKey( key ).build() );
            }
            catch ( Exception e )
            {
                log.warn( "Failed starting app: " + e.getMessage() );
            }
        }

        // Try to stop
        if ( !enabled && !state.equals( "stopped" ) )
        {
            try
            {
                xpClientCache.stop( namespace ).accept( ImmutableAppKeyList.builder().addKey( key ).build() );
            }
            catch ( Exception e )
            {
                log.warn( "Failed stopping app: " + e.getMessage() );
            }
        }
    }
}
