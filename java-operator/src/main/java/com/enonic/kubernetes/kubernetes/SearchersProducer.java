package com.enonic.kubernetes.kubernetes;

import javax.inject.Singleton;

import io.micronaut.context.annotation.Factory;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

@Factory
public class SearchersProducer
{
    @Singleton
    Searchers searchers( Informers informers )
    {
        singletonAssert(this, "createSearchers");
        return SearchersImpl.builder().
            configMap( new InformerSearcher<>( informers.configMapInformer() ) ).
            ingress( new InformerSearcher<>( informers.ingressInformer() ) ).
            namespace( new InformerSearcher<>( informers.namespaceInformer() ) ).
            pod( new InformerSearcher<>( informers.podInformer() ) ).
            xp7Config( new InformerSearcher<>( informers.xp7ConfigInformer() ) ).
            xp7Deployment( new InformerSearcher<>( informers.xp7DeploymentInformer() ) ).
            event( new InformerSearcher<>( informers.eventInformer() ) ).
            build();
    }
}
