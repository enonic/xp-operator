package com.enonic.kubernetes.operator.v1alpha1xp7app;

import com.enonic.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Comparators.namespaceAndName;
import static com.enonic.kubernetes.kubernetes.Predicates.fieldsEquals;
import static com.enonic.kubernetes.kubernetes.Predicates.hasFinalizer;
import static com.enonic.kubernetes.kubernetes.Predicates.isDeleted;
import static com.enonic.kubernetes.kubernetes.Predicates.isNotDeleted;
import static com.enonic.kubernetes.operator.v1alpha1xp7app.Predicates.notSuccessfullyInstalled;

/**
 * This operator class installs/uninstalls apps in XP
 */
@ApplicationScoped
public class OperatorXp7AppInstaller
    extends InformerEventHandler<Xp7App>
    implements Runnable
{
    @Inject
    Searchers searchers;

    @Inject
    HandlerInstall handlerInstall;

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
        handlerInstall.installApp( newResource );
    }

    @Override
    public void onUpdate( final Xp7App oldResource, final Xp7App newResource )
    {
        // If app marked for deletion
        if (isDeleted().test( newResource )) {
            handlerInstall.uninstallApp( newResource );
        }

        // If url or sha512 has changed
        if (!fieldsEquals( oldResource, r -> r.getSpec().getUrl(), r -> r.getSpec().getSha512() ).test( newResource )) {
            // Try to reinstall app
            handlerInstall.triggerAppReinstall( newResource );
        }
    }

    @Override
    public void onDelete( final Xp7App oldResource, final boolean b )
    {
        // Handled by finalizers
    }

    @Override
    public void run()
    {
        // Runs periodically to ensure state
        searchers.xp7App()
            .filter( isNotDeleted() )
            .filter( hasFinalizer( cfgStr( "operator.charts.values.finalizers.app.uninstall" ) ) )
            .filter( notSuccessfullyInstalled() )
            .sorted( namespaceAndName() )
            .forEach( handlerInstall::installApp );

        searchers.xp7App()
            .filter( isDeleted() )
            .filter( hasFinalizer( cfgStr( "operator.charts.values.finalizers.app.uninstall" ) ) )
            .sorted( namespaceAndName() )
            .forEach( handlerInstall::uninstallApp );
    }
}
