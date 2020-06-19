package com.enonic.cloud.operator;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.TaskRunner;
import com.enonic.cloud.kubernetes.Informers;
import com.enonic.cloud.operator.dns.OperatorDns;
import com.enonic.cloud.operator.helpers.InformerEventHandler;
import com.enonic.cloud.operator.v1alpha1xp7app.OperatorXp7AppInstaller;
import com.enonic.cloud.operator.v1alpha1xp7app.OperatorXp7AppStatus;
import com.enonic.cloud.operator.v1alpha2xp7config.OperatorConfigMapSync;
import com.enonic.cloud.operator.v1alpha2xp7config.OperatorXp7Config;
import com.enonic.cloud.operator.v1alpha2xp7deployment.OperatorNamespaceDelete;
import com.enonic.cloud.operator.v1alpha2xp7deployment.OperatorXp7DeploymentHelm;
import com.enonic.cloud.operator.v1alpha2xp7deployment.OperatorXp7DeploymentStatus;
import com.enonic.cloud.operator.v1alpha2xp7deployment.OperatorXpClientCacheInvalidate;
import com.enonic.cloud.operator.v1alpha2xp7vhost.OperatorXp7ConfigSync;
import com.enonic.cloud.operator.v1alpha2xp7vhost.OperatorXp7VHost;
import com.enonic.cloud.operator.v1alpha2xp7vhost.OperatorXp7VHostHelm;
import com.enonic.cloud.operator.v1alpha2xp7vhost.OperatorXp7VHostStatus;

import static com.enonic.cloud.common.Configuration.cfgIfBool;
import static com.enonic.cloud.common.Configuration.cfgLong;

@ApplicationScoped
public class Operator
{
    private static final Logger log = LoggerFactory.getLogger( Operator.class );

    private final TaskRunner taskRunner;

    private final Informers informers;

    private final OperatorDns operatorDns;

    private final OperatorXp7AppInstaller operatorXp7AppInstaller;

    private final OperatorXp7AppStatus operatorXp7AppStatus;

    private final OperatorXp7Config operatorXp7Config;

    private final OperatorConfigMapSync operatorConfigMapSync;

    private final OperatorNamespaceDelete operatorNamespaceDelete;

    private final OperatorXp7DeploymentHelm operatorXp7DeploymentHelm;

    private final OperatorXp7DeploymentStatus operatorXp7DeploymentStatus;

    private final OperatorXpClientCacheInvalidate operatorXpClientCacheInvalidate;

    private final OperatorXp7VHostHelm operatorXp7VHostHelm;

    private final OperatorXp7VHost operatorXp7VHost;

    private final OperatorXp7VHostStatus operatorXp7VHostStatus;

    private final OperatorXp7ConfigSync operatorXp7ConfigSync;

    @Inject
    public Operator( final TaskRunner taskRunner, final Informers informers, final OperatorDns operatorDns,
                     final OperatorXp7AppInstaller operatorXp7AppInstaller, final OperatorXp7AppStatus operatorXp7AppStatus,
                     final OperatorXp7Config operatorXp7Config, final OperatorConfigMapSync operatorConfigMapSync,
                     final OperatorNamespaceDelete operatorNamespaceDelete, final OperatorXp7DeploymentHelm operatorXp7DeploymentHelm,
                     final OperatorXp7DeploymentStatus operatorXp7DeploymentStatus,
                     final OperatorXpClientCacheInvalidate operatorXpClientCacheInvalidate, final OperatorXp7VHostHelm operatorXp7VHostHelm,
                     final OperatorXp7VHost operatorXp7VHost, final OperatorXp7VHostStatus operatorXp7VHostStatus,
                     final OperatorXp7ConfigSync operatorXp7ConfigSync )
    {
        this.taskRunner = taskRunner;
        this.informers = informers;
        this.operatorDns = operatorDns;
        this.operatorXp7AppInstaller = operatorXp7AppInstaller;
        this.operatorXp7AppStatus = operatorXp7AppStatus;
        this.operatorXp7Config = operatorXp7Config;
        this.operatorConfigMapSync = operatorConfigMapSync;
        this.operatorNamespaceDelete = operatorNamespaceDelete;
        this.operatorXp7DeploymentHelm = operatorXp7DeploymentHelm;
        this.operatorXp7DeploymentStatus = operatorXp7DeploymentStatus;
        this.operatorXpClientCacheInvalidate = operatorXpClientCacheInvalidate;
        this.operatorXp7VHostHelm = operatorXp7VHostHelm;
        this.operatorXp7VHost = operatorXp7VHost;
        this.operatorXp7VHostStatus = operatorXp7VHostStatus;
        this.operatorXp7ConfigSync = operatorXp7ConfigSync;
    }

    void onStartup( @Observes StartupEvent _ev )
    {
        long statusInterval = cfgLong( "operator.tasks.status.interval" );
        long syncInterval = cfgLong( "operator.tasks.sync.interval" );

        cfgIfBool( "dns.enabled", () -> {
            listen( operatorDns, informers.ingressInformer() );
        } );

        listen( operatorXp7AppInstaller, informers.xp7AppInformer() );
        schedule( operatorXp7AppInstaller, syncInterval );
        schedule( operatorXp7AppStatus, statusInterval );

        listen( operatorXp7Config, informers.xp7ConfigInformer() );
        schedule( operatorConfigMapSync, syncInterval );

        listen( operatorNamespaceDelete, informers.xp7DeploymentInformer() );
        listen( operatorXp7DeploymentHelm, informers.xp7DeploymentInformer() );
        schedule( operatorXp7DeploymentStatus, statusInterval );
        listen( operatorXpClientCacheInvalidate, informers.xp7DeploymentInformer() );

        listen( operatorXp7VHostHelm, informers.xp7VHostInformer() );
        listen( operatorXp7VHost, informers.xp7VHostInformer() );
        schedule( operatorXp7VHostStatus, statusInterval );
        schedule( operatorXp7ConfigSync, syncInterval );

        log.info( "Starting informers" );
        informers.informerFactory().startAllRegisteredInformers();
    }

    private <T> void listen( ResourceEventHandler<T> handler, SharedIndexInformer<T> informer )
    {
        log.info( String.format( "Adding listener '%s'", handler.getClass().getSimpleName() ) );
        if ( handler instanceof InformerEventHandler )
        {
            ( (InformerEventHandler) handler ).initialize();
        }
        informer.addEventHandler( handler );
    }

    private void schedule( Runnable runnable, long periodMs )
    {
        long leftLimit = 1000L;
        long rightLimit = 10000L;
        long initialDelayMs = leftLimit + (long) ( Math.random() * ( rightLimit - leftLimit ) );
        log.info( String.format( "Adding schedule '%s' [delay: %d, period: %d]", runnable.getClass().getSimpleName(), initialDelayMs,
                                 periodMs ) );
        taskRunner.scheduleAtFixedRate( runnable, initialDelayMs, periodMs, TimeUnit.MILLISECONDS );
    }
}
