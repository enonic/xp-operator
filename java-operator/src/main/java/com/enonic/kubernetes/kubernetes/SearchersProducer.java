package com.enonic.kubernetes.kubernetes;

import jakarta.inject.Singleton;

import io.micronaut.context.annotation.Factory;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

@Factory
public class SearchersProducer
{
    @Singleton
    Searchers searchers( Informers informers )
    {
        singletonAssert(this, "createSearchers");
        return new Searchers( new InformerSearcher<>( informers.configMapInformer() ),
                              new InformerSearcher<>( informers.ingressInformer() ),
                              new InformerSearcher<>( informers.namespaceInformer() ),
                              new InformerSearcher<>( informers.podInformer() ),
                              new InformerSearcher<>( informers.xp8ConfigInformer() ),
                              new InformerSearcher<>( informers.xp8DeploymentInformer() ),
                              new InformerSearcher<>( informers.eventInformer() ) );
    }
}
