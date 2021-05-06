package com.enonic.kubernetes.operator;

import com.enonic.kubernetes.common.TaskRunner;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.operator.domain.OperatorDomainCertSync;
import com.enonic.kubernetes.operator.domain.OperatorDomainDns;
import com.enonic.kubernetes.operator.domain.OperatorIngressCertSync;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import com.enonic.kubernetes.operator.ingress.OperatorIngress;
import com.enonic.kubernetes.operator.ingress.OperatorIngressLabel;
import com.enonic.kubernetes.operator.ingress.OperatorXp7ConfigSync;
import com.enonic.kubernetes.operator.v1alpha1xp7app.OperatorXp7AppInstaller;
import com.enonic.kubernetes.operator.v1alpha1xp7app.OperatorXp7AppInstallerOnDeployments;
import com.enonic.kubernetes.operator.v1alpha1xp7app.OperatorXp7AppStartStopper;
import com.enonic.kubernetes.operator.v1alpha1xp7app.OperatorXp7AppStatus;
import com.enonic.kubernetes.operator.v1alpha1xp7app.OperatorXp7AppStatusOnDeployments;
import com.enonic.kubernetes.operator.v1alpha2xp7config.OperatorConfigMapEvent;
import com.enonic.kubernetes.operator.v1alpha2xp7config.OperatorConfigMapSync;
import com.enonic.kubernetes.operator.v1alpha2xp7config.OperatorXp7Config;
import com.enonic.kubernetes.operator.v1alpha2xp7config.OperatorXp7ConfigStatus;
import com.enonic.kubernetes.operator.v1alpha2xp7deployment.OperatorDeleteAnnotation;
import com.enonic.kubernetes.operator.v1alpha2xp7deployment.OperatorXp7DeploymentHelm;
import com.enonic.kubernetes.operator.v1alpha2xp7deployment.OperatorXp7DeploymentStatus;
import com.enonic.kubernetes.operator.v1alpha2xp7deployment.OperatorXpClientCacheInvalidate;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import static com.enonic.kubernetes.common.Configuration.cfgIfBool;
import static com.enonic.kubernetes.common.Configuration.cfgLong;

/**
 * This is the entry point for the Operator, it sets up all the Operator classes
 */
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

    private final OperatorIngressLabel operatorIngressLabel;

    private final OperatorXp7ConfigSync operatorXp7ConfigSync;

    private final OperatorXp7AppInstaller operatorXp7AppInstaller;

    private final OperatorXp7AppInstallerOnDeployments operatorXp7AppInstallerOnDeployments;

    private final OperatorXp7AppStartStopper operatorXp7AppStartStopper;

    private final OperatorXp7AppStatus operatorXp7AppStatus;

    private final OperatorXp7AppStatusOnDeployments operatorXp7AppStatusOnDeployments;

    private final OperatorXp7Config operatorXp7Config;

    private final OperatorXp7ConfigStatus operatorXp7ConfigStatus;

    private final OperatorConfigMapEvent operatorConfigMapEvent;

    private final OperatorConfigMapSync operatorConfigMapSync;

    private final OperatorDeleteAnnotation operatorDeleteAnnotation;

    private final OperatorXp7DeploymentHelm operatorXp7DeploymentHelm;

    private final OperatorXp7DeploymentStatus operatorXp7DeploymentStatus;

    private final OperatorXpClientCacheInvalidate operatorXpClientCacheInvalidate;

    @Inject
    public Operator( final TaskRunner taskRunner,
                     final Informers informers,
                     final OperatorDomainCertSync operatorDomainCertSync,
                     final OperatorDomainDns operatorDomainDns,
                     final OperatorIngressCertSync operatorIngressCertSync,
                     final OperatorIngress operatorIngress,
                     final OperatorIngressLabel operatorIngressLabel,
                     final OperatorXp7ConfigSync operatorXp7ConfigSync,
                     final OperatorXp7AppInstaller operatorXp7AppInstaller,
                     final OperatorXp7AppInstallerOnDeployments operatorXp7AppInstallerOnDeployments,
                     final OperatorXp7AppStatus operatorXp7AppStatus,
                     final OperatorXp7AppStatusOnDeployments operatorXp7AppStatusOnDeployments,
                     final OperatorXp7Config operatorXp7Config,
                     final OperatorXp7ConfigStatus operatorXp7ConfigStatus,
                     final OperatorConfigMapEvent operatorConfigMapEvent,
                     final OperatorConfigMapSync operatorConfigMapSync,
                     final OperatorDeleteAnnotation operatorDeleteAnnotation,
                     final OperatorXp7DeploymentHelm operatorXp7DeploymentHelm,
                     final OperatorXp7AppStartStopper operatorXp7AppStartStopper,
                     final OperatorXp7DeploymentStatus operatorXp7DeploymentStatus,
                     final OperatorXpClientCacheInvalidate operatorXpClientCacheInvalidate )
    {
        this.taskRunner = taskRunner;
        this.informers = informers;
        this.operatorDomainCertSync = operatorDomainCertSync;
        this.operatorDomainDns = operatorDomainDns;
        this.operatorIngressCertSync = operatorIngressCertSync;
        this.operatorIngress = operatorIngress;
        this.operatorIngressLabel = operatorIngressLabel;
        this.operatorXp7ConfigSync = operatorXp7ConfigSync;
        this.operatorXp7AppInstaller = operatorXp7AppInstaller;
        this.operatorXp7AppInstallerOnDeployments = operatorXp7AppInstallerOnDeployments;
        this.operatorXp7AppStatus = operatorXp7AppStatus;
        this.operatorXp7AppStatusOnDeployments = operatorXp7AppStatusOnDeployments;
        this.operatorXp7Config = operatorXp7Config;
        this.operatorXp7ConfigStatus = operatorXp7ConfigStatus;
        this.operatorConfigMapEvent = operatorConfigMapEvent;
        this.operatorConfigMapSync = operatorConfigMapSync;
        this.operatorDeleteAnnotation = operatorDeleteAnnotation;
        this.operatorXp7DeploymentHelm = operatorXp7DeploymentHelm;
        this.operatorXp7AppStartStopper = operatorXp7AppStartStopper;
        this.operatorXp7DeploymentStatus = operatorXp7DeploymentStatus;
        this.operatorXpClientCacheInvalidate = operatorXpClientCacheInvalidate;
    }

    void onStartup( @Observes StartupEvent _ev )
    {
        log.info( "Starting api and other components" );

        // The timer is here to give the operator api time to start up
        new Timer().schedule( new java.util.TimerTask()
        {
            @Override
            public void run()
            {
                log.info( "Starting schedules and other components" );

                long syncInterval = cfgLong( "operator.tasks.sync.interval" );

                listen( operatorDomainCertSync, informers.domainInformer() );
                cfgIfBool( "dns.enabled", () -> {
                    listen( operatorDomainDns, informers.domainInformer() );
                } );
                listen( operatorIngressLabel, informers.xp7ConfigInformer() );
                schedule( operatorIngressLabel, syncInterval );
                listen( operatorIngressCertSync, informers.ingressInformer() );

                listen( operatorIngress, informers.ingressInformer() );
                schedule( operatorXp7ConfigSync, syncInterval );

                listen( operatorXp7AppInstaller, informers.xp7AppInformer() );
                schedule( operatorXp7AppInstaller, syncInterval );
                listen( operatorXp7AppInstallerOnDeployments, informers.xp7DeploymentInformer() );
                listen( operatorXp7AppStartStopper, informers.xp7AppInformer() );
                schedule( operatorXp7AppStartStopper, syncInterval );

                schedule( operatorXp7AppStatus, syncInterval );
                listen( operatorXp7AppStatusOnDeployments, informers.xp7DeploymentInformer() );

                listen( operatorXp7Config, informers.xp7ConfigInformer() );
                listen( operatorConfigMapEvent, informers.configMapInformer() );
                schedule( operatorConfigMapSync, syncInterval );
                listen( operatorXp7ConfigStatus, informers.eventInformer() );

                listen( operatorDeleteAnnotation, informers.xp7DeploymentInformer() );
                listen( operatorXp7DeploymentHelm, informers.xp7DeploymentInformer() );
                listen( operatorXp7DeploymentStatus, informers.podInformer() );
                schedule( operatorXp7DeploymentStatus, syncInterval );
                listen( operatorXpClientCacheInvalidate, informers.xp7DeploymentInformer() );

                log.info( "Starting informers" );
                informers.informerFactory().startAllRegisteredInformers();
            }
        }, 10000 );
    }

    private <T> void listen( ResourceEventHandler<T> handler, SharedIndexInformer<T> informer )
    {
        log.info( String.format( "Adding listener '%s'", handler.getClass().getSimpleName() ) );
        if (handler instanceof InformerEventHandler) {
            ((InformerEventHandler) handler).initialize();
        }
        informer.addEventHandler( handler );
    }

    private void schedule( Runnable runnable, long periodMs )
    {
        long leftLimit = 1000L;
        long rightLimit = 10000L;
        long initialDelayMs = leftLimit + (long) (Math.random() * (rightLimit - leftLimit));
        log.info( String.format( "Adding schedule '%s' [delay: %d, period: %d]", runnable.getClass().getSimpleName(), initialDelayMs,
            periodMs ) );
        runnable.run();
        taskRunner.scheduleAtFixedRate( runnable, initialDelayMs, periodMs, TimeUnit.MILLISECONDS );
    }
}
