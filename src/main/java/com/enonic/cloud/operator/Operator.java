package com.enonic.cloud.operator;

import java.util.Timer;
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
import com.enonic.cloud.operator.domain.OperatorDomainCertSync;
import com.enonic.cloud.operator.domain.OperatorDomainDns;
import com.enonic.cloud.operator.domain.OperatorIngressCertSync;
import com.enonic.cloud.operator.helpers.InformerEventHandler;
import com.enonic.cloud.operator.ingress.OperatorIngress;
import com.enonic.cloud.operator.ingress.OperatorXp7ConfigSync;
import com.enonic.cloud.operator.v1alpha1xp7app.OperatorXp7AppInstaller;
import com.enonic.cloud.operator.v1alpha1xp7app.OperatorXp7AppStartStopper;
import com.enonic.cloud.operator.v1alpha1xp7app.OperatorXp7AppStatus;
import com.enonic.cloud.operator.v1alpha2xp7config.OperatorConfigMapSync;
import com.enonic.cloud.operator.v1alpha2xp7config.OperatorXp7Config;
import com.enonic.cloud.operator.v1alpha2xp7deployment.OperatorNamespaceDelete;
import com.enonic.cloud.operator.v1alpha2xp7deployment.OperatorXp7DeploymentHelm;
import com.enonic.cloud.operator.v1alpha2xp7deployment.OperatorXp7DeploymentStatus;
import com.enonic.cloud.operator.v1alpha2xp7deployment.OperatorXpClientCacheInvalidate;
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

    private final OperatorDomainCertSync operatorDomainCertSync;

    private final OperatorDomainDns operatorDomainDns;

    private final OperatorIngressCertSync operatorIngressCertSync;

    private final OperatorIngress operatorIngress;

    private final OperatorXp7ConfigSync operatorXp7ConfigSync;

    private final OperatorXp7AppInstaller operatorXp7AppInstaller;

    private final OperatorXp7AppStartStopper operatorXp7AppStartStopper;

    private final OperatorXp7AppStatus operatorXp7AppStatus;

    private final OperatorXp7Config operatorXp7Config;

    private final OperatorConfigMapSync operatorConfigMapSync;

    private final OperatorNamespaceDelete operatorNamespaceDelete;

    private final OperatorXp7DeploymentHelm operatorXp7DeploymentHelm;

    private final OperatorXp7DeploymentStatus operatorXp7DeploymentStatus;

    private final OperatorXpClientCacheInvalidate operatorXpClientCacheInvalidate;

    private final OperatorXp7VHostHelm operatorXp7VHostHelm;

    private final OperatorXp7VHostStatus operatorXp7VHostStatus;

    @Inject
    public Operator( final TaskRunner taskRunner, final Informers informers, final OperatorDomainCertSync operatorDomainCertSync,
                     final OperatorDomainDns operatorDomainDns, final OperatorIngressCertSync operatorIngressCertSync,
                     final OperatorIngress operatorIngress, final OperatorXp7ConfigSync operatorXp7ConfigSync,
                     final OperatorXp7AppInstaller operatorXp7AppInstaller, final OperatorXp7AppStatus operatorXp7AppStatus,
                     final OperatorXp7Config operatorXp7Config, final OperatorConfigMapSync operatorConfigMapSync,
                     final OperatorNamespaceDelete operatorNamespaceDelete, final OperatorXp7DeploymentHelm operatorXp7DeploymentHelm,
                     final OperatorXp7AppStartStopper operatorXp7AppStartStopper,
                     final OperatorXp7DeploymentStatus operatorXp7DeploymentStatus,
                     final OperatorXpClientCacheInvalidate operatorXpClientCacheInvalidate, final OperatorXp7VHostHelm operatorXp7VHostHelm,
                     final OperatorXp7VHostStatus operatorXp7VHostStatus )
    {
        this.taskRunner = taskRunner;
        this.informers = informers;
        this.operatorDomainCertSync = operatorDomainCertSync;
        this.operatorDomainDns = operatorDomainDns;
        this.operatorIngressCertSync = operatorIngressCertSync;
        this.operatorIngress = operatorIngress;
        this.operatorXp7ConfigSync = operatorXp7ConfigSync;
        this.operatorXp7AppInstaller = operatorXp7AppInstaller;
        this.operatorXp7AppStatus = operatorXp7AppStatus;
        this.operatorXp7Config = operatorXp7Config;
        this.operatorConfigMapSync = operatorConfigMapSync;
        this.operatorNamespaceDelete = operatorNamespaceDelete;
        this.operatorXp7DeploymentHelm = operatorXp7DeploymentHelm;
        this.operatorXp7AppStartStopper = operatorXp7AppStartStopper;
        this.operatorXp7DeploymentStatus = operatorXp7DeploymentStatus;
        this.operatorXpClientCacheInvalidate = operatorXpClientCacheInvalidate;
        this.operatorXp7VHostHelm = operatorXp7VHostHelm;
        this.operatorXp7VHostStatus = operatorXp7VHostStatus;
    }

    void onStartup( @Observes StartupEvent _ev )
    {
        // The timer is here to give the operator api time to start up
        new Timer().schedule( new java.util.TimerTask()
        {
            @Override
            public void run()
            {
                long statusInterval = cfgLong( "operator.tasks.status.interval" );
                long syncInterval = cfgLong( "operator.tasks.sync.interval" );

                listen( operatorDomainCertSync, informers.domainInformer() );
                cfgIfBool( "dns.enabled", () -> {
                    listen( operatorDomainDns, informers.domainInformer() );
                } );
                listen( operatorIngressCertSync, informers.ingressInformer() );

                listen( operatorIngress, informers.ingressInformer() );
                schedule( operatorXp7ConfigSync, syncInterval );

                listen( operatorXp7AppInstaller, informers.xp7AppInformer() );
                schedule( operatorXp7AppInstaller, syncInterval );
                listen( operatorXp7AppStartStopper, informers.xp7AppInformer() );
                schedule( operatorXp7AppStartStopper, statusInterval );
                schedule( operatorXp7AppStatus, statusInterval );

                listen( operatorXp7Config, informers.xp7ConfigInformer() );
                schedule( operatorConfigMapSync, syncInterval );

                listen( operatorNamespaceDelete, informers.xp7DeploymentInformer() );
                listen( operatorXp7DeploymentHelm, informers.xp7DeploymentInformer() );
                schedule( operatorXp7DeploymentStatus, statusInterval );
                listen( operatorXpClientCacheInvalidate, informers.xp7DeploymentInformer() );

                listen( operatorXp7VHostHelm, informers.xp7VHostInformer() );
                schedule( operatorXp7VHostStatus, statusInterval );

                log.info( "Starting informers" );
                informers.informerFactory().startAllRegisteredInformers();
            }
        }, 5000 );
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
