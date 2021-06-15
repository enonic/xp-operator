package com.enonic.kubernetes.operator.v1alpha1xp7app;

import com.enonic.kubernetes.apis.xp.XpClientCache;
import com.enonic.kubernetes.apis.xp.XpClientException;
import com.enonic.kubernetes.apis.xp.service.AppEvent;
import com.enonic.kubernetes.apis.xp.service.AppEventType;
import com.enonic.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.operator.Operator;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.enonic.kubernetes.common.Configuration.cfgLong;
import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.kubernetes.Comparators.namespaceAndName;
import static com.enonic.kubernetes.kubernetes.Predicates.hasFinalizer;
import static com.enonic.kubernetes.kubernetes.Predicates.inNamespace;
import static com.enonic.kubernetes.kubernetes.Predicates.isNotDeleted;
import static com.enonic.kubernetes.kubernetes.Predicates.isPartOfDeployment;
import static com.enonic.kubernetes.operator.v1alpha1xp7app.Predicates.matchesKey;
import static com.enonic.kubernetes.operator.v1alpha1xp7app.Predicates.successfullyInstalled;
import static com.enonic.kubernetes.operator.v1alpha1xp7app.Utils.triggerAppReinstall;
import static com.enonic.kubernetes.operator.v1alpha2xp7deployment.Predicates.running;

/**
 * This operator class updates Xp7App status fields
 */
@ApplicationScoped
public class OperatorXp7AppStatus
    implements Runnable,
    Consumer<AppEvent>
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7AppStatus.class );

    @Inject
    Operator operator;

    @Inject
    HandlerStatus handlerStatus;

    @Inject
    Clients clients;

    @Inject
    Searchers searchers;

    @Inject
    XpClientCache xpClientCache;

    void onStart( @Observes StartupEvent ev )
    {
        operator.schedule( cfgLong( "operator.tasks.sync.interval" ), this );
    }

    @Override
    public void run()
    {
        List<Xp7Deployment> deployments = searchers.xp7Deployment().stream().collect( Collectors.toList() );

        for (Xp7Deployment deployment : deployments) {
            // Make sure status on apps is up to date
            searchers.xp7App()
                .filter( isPartOfDeployment( deployment ) )
                .filter( isNotDeleted() )
                .filter( hasFinalizer( cfgStr( "operator.charts.values.finalizers.app.uninstall" ) ) )
                .filter( successfullyInstalled() )
                .sorted( namespaceAndName() )
                .forEach( app -> handlerStatus.updateStatus( deployment, app ) );

            // Make sure there is an app listener on all deployments
            if (running().test( deployment )) {
                try {
                    xpClientCache.appAddListener( deployment.getMetadata().getNamespace(), this );
                } catch (XpClientException e) {
                    log.warn( String.format( "Failed adding app status listener in NS %s: %s", deployment.getMetadata().getNamespace(),
                        e.getMessage() ) );
                }
            }
        }
    }

    @Override
    public void accept( final AppEvent appEvent )
    {
        // Match with k8s resource
        Optional<Xp7App> optionalXp7App = searchers.xp7App().find(
            inNamespace( appEvent.namespace() ),
            matchesKey( appEvent.key() )
        );

        // If no match is found, exit
        if (optionalXp7App.isEmpty()) {
            return;
        }
        Xp7App app = optionalXp7App.get();

        // If app has been uninstalled but not deleted in K8s
        if (appEvent.type() == AppEventType.UNINSTALLED) {
            if (isNotDeleted().test( app )) {
                triggerAppReinstall( clients, app );
            }
            return;
        }

        // If app version in XP does not match K8s
        if (!appEvent.info().version().equals( app.getStatus().getXp7AppStatusFields().getXp7AppStatusFieldsAppInfo().getVersion() )) {
            triggerAppReinstall( clients, app );
            return;
        }

        // Else update status
        handlerStatus.updateStatus( app, appEvent );
    }
}
