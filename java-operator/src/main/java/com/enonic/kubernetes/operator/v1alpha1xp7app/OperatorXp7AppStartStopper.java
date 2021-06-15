package com.enonic.kubernetes.operator.v1alpha1xp7app;

import com.enonic.kubernetes.apis.xp.XpClientCache;
import com.enonic.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Comparators.namespaceAndName;
import static com.enonic.kubernetes.kubernetes.Predicates.hasFinalizer;
import static com.enonic.kubernetes.kubernetes.Predicates.isNotDeleted;
import static com.enonic.kubernetes.operator.v1alpha1xp7app.Predicates.notSuccessfullyInstalled;
import static com.enonic.kubernetes.operator.v1alpha2xp7deployment.Predicates.parent;
import static com.enonic.kubernetes.operator.v1alpha2xp7deployment.Predicates.running;


/**
 * This operator class starts/stops apps in XP
 */
@ApplicationScoped
public class OperatorXp7AppStartStopper
    extends InformerEventHandler<Xp7App>
    implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7AppStartStopper.class );

    @Inject
    Searchers searchers;

    @Inject
    XpClientCache xpClientCache;

    @Inject
    Informers informers;

    void onStart( @Observes StartupEvent ev )
    {
        listen( informers.xp7AppInformer() );
        scheduleSync( this );
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
        // Periodically make sure state is consistent
        searchers.xp7App()
            .filter( isNotDeleted() )
            .filter( hasFinalizer( cfgStr( "operator.charts.values.finalizers.app.uninstall" ) ) )
            .sorted( namespaceAndName() )
            .forEach( this::handle );
    }

    private void handle( final Xp7App xp7App )
    {
        // Bail if app has not been installed
        if (notSuccessfullyInstalled().test( xp7App )) {
            return;
        }

        // XP is not running, nothing we can do
        if (!searchers.xp7Deployment().match( parent( xp7App ), running() )) {
            return;
        }

        // Collect info
        Boolean enabled = xp7App.getSpec().getEnabled();
        String state = xp7App.getStatus()
            .getXp7AppStatusFields()
            .getXp7AppStatusFieldsAppInfo()
            .getState();
        String key = xp7App.getStatus()
            .getXp7AppStatusFields()
            .getXp7AppStatusFieldsAppInfo()
            .getKey();
        String namespace = xp7App.getMetadata().getNamespace();

        // Try to start
        if (enabled && !state.equals( "started" )) {
            try {
                xpClientCache.appStart( namespace, key );
            } catch (Exception e) {
                log.warn( String.format( "XP: Failed starting app %s in NS %s: %s", xp7App.getMetadata().getName(),
                    xp7App.getMetadata().getNamespace(), e.getMessage() ) );
            }
        }

        // Try to stop
        if (!enabled && !state.equals( "stopped" )) {
            try {
                xpClientCache.appStop( namespace, key );
            } catch (Exception e) {
                log.warn( String.format( "XP: Failed starting app %s in NS %s: %s", xp7App.getMetadata().getName(),
                    xp7App.getMetadata().getNamespace(), e.getMessage() ) );
            }
        }
    }
}
