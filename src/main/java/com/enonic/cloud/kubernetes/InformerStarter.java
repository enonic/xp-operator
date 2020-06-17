package com.enonic.cloud.kubernetes;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;

@ApplicationScoped
public class InformerStarter
{
    private static final Logger log = LoggerFactory.getLogger( InformerStarter.class );

    private static final Long startingDelay = 1 * 1000L; // TODO: Add to properties file

    @Inject
    SharedInformerFactory factory;

    @Inject
    SharedIndexInformer<ConfigMap> configMapSharedIndexInformer;

    @Inject
    SharedIndexInformer<Ingress> ingressSharedIndexInformer;

    @Inject
    SharedIndexInformer<Pod> podSharedIndexInformer;

    @Inject
    SharedIndexInformer<Xp7App> xp7AppSharedIndexInformer;

    @Inject
    SharedIndexInformer<Xp7Config> xp7ConfigSharedIndexInformer;

    @Inject
    SharedIndexInformer<Xp7Deployment> xp7DeploymentSharedIndexInformer;

    @Inject
    SharedIndexInformer<Xp7VHost> xp7VHostSharedIndexInformer;

    void onStartup( @Observes StartupEvent _ev )
    {
        log.info( "Starting informers" );
        factory.startAllRegisteredInformers();
    }
}
