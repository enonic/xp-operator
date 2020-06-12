package com.enonic.cloud.kubernetes;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;

public class InformerSearchersProducer
{
    @Singleton
    @Produces
    InformerSearcher<ConfigMap> configMapInformerSearcher( SharedIndexInformer<ConfigMap> informer )
    {
        return new InformerSearcher<>( informer );
    }

    @Singleton
    @Produces
    InformerSearcher<Ingress> ingressInformerSearcher( SharedIndexInformer<Ingress> informer )
    {
        return new InformerSearcher<>( informer );
    }

    @Singleton
    @Produces
    InformerSearcher<Pod> podInformerSearcher( SharedIndexInformer<Pod> informer )
    {
        return new InformerSearcher<>( informer );
    }

    @Singleton
    @Produces
    InformerSearcher<Xp7App> xp7AppInformerSearcher( SharedIndexInformer<Xp7App> informer )
    {
        return new InformerSearcher<>( informer );
    }

    @Singleton
    @Produces
    InformerSearcher<Xp7Config> xp7ConfigInformerSearcher( SharedIndexInformer<Xp7Config> informer )
    {
        return new InformerSearcher<>( informer );
    }

    @Singleton
    @Produces
    InformerSearcher<Xp7Deployment> xp7DeploymentInformerSearcher( SharedIndexInformer<Xp7Deployment> informer )
    {
        return new InformerSearcher<>( informer );
    }

    @Singleton
    @Produces
    InformerSearcher<Xp7VHost> xp7VHostInformerSearcher( SharedIndexInformer<Xp7VHost> informer )
    {
        return new InformerSearcher<>( informer );
    }
}
