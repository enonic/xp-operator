package com.enonic.cloud.operator.v1alpha1xp7app;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.kubernetes.Comparators.namespaceAndName;
import static com.enonic.cloud.kubernetes.Predicates.hasFinalizer;
import static com.enonic.cloud.kubernetes.Predicates.isDeleted;


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
        searchers.xp7App().stream().
            filter( isDeleted().negate() ).
            filter( hasFinalizer( cfgStr( "operator.charts.values.finalizers.app.uninstall" ) ) ).
            sorted( namespaceAndName() ).
            forEach( this::handle );
    }

    private synchronized void handle( final Xp7App xp7App )
    {
        // If app info is missing
        if ( xp7App.getStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo() == null )
        {
            return;
        }

        // Collect info
        Boolean enabled = xp7App.getSpec().getEnabled();
        String state = xp7App.getStatus().
            getXp7AppStatusFields().
            getXp7AppStatusFieldsAppInfo().
            getState();
        String key = xp7App.getStatus().
            getXp7AppStatusFields().
            getXp7AppStatusFieldsAppInfo().
            getKey();
        String namespace = xp7App.getMetadata().getNamespace();

        // Try to start
        if ( enabled && !state.equals( "started" ) )
        {
            try
            {
                xpClientCache.appStart( namespace, key );
            }
            catch ( Exception e )
            {
                log.warn( String.format( "XP: Failed starting app %s in NS %s: %s", xp7App.getMetadata().getName(),
                                         xp7App.getMetadata().getNamespace(), e.getMessage() ) );
            }
        }

        // Try to stop
        if ( !enabled && !state.equals( "stopped" ) )
        {
            try
            {
                xpClientCache.appStop( namespace, key );
            }
            catch ( Exception e )
            {
                log.warn( String.format( "XP: Failed starting app %s in NS %s: %s", xp7App.getMetadata().getName(),
                                         xp7App.getMetadata().getNamespace(), e.getMessage() ) );
            }
        }
    }
}
