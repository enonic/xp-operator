package com.enonic.cloud.operator.v1alpha1xp7app;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.apis.xp.XpClientCache;
import com.enonic.cloud.apis.xp.service.ImmutableAppKeyList;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;


@Singleton
public class OperatorXp7AppStartStopper
    extends InformerEventHandler<Xp7App>
    implements Runnable
{
    @Inject
    Searchers searchers;

    @Inject
    XpClientCache xpClientCache;

    @Override
    protected void init()
    {
        // Do nothing
    }

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
        searchers.xp7App().query().
            hasNotBeenDeleted().
            hasFinalizer( cfgStr( "operator.finalizer.app.uninstall" ) ).
            forEach( this::handle );
    }

    private void handle( final Xp7App xp7App )
    {
        if ( xp7App.getXp7AppStatus() == null || xp7App.getXp7AppStatus().getXp7AppStatusFields() == null ||
            xp7App.getXp7AppStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo() == null )
        {
            return;
        }

        if ( xp7App.getXp7AppSpec().getEnabled() == null )
        {
            return;
        }

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

        if ( enabled && !state.equals( "started" ) )
        {
            xpClientCache.start( namespace ).accept( ImmutableAppKeyList.builder().addKey( key ).build() );
        }
        else if ( !enabled && !state.equals( "stopped" ) )
        {
            xpClientCache.stop( namespace ).accept( ImmutableAppKeyList.builder().addKey( key ).build() );
        }
    }
}
