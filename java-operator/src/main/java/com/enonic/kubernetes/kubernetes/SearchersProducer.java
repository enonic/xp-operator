package com.enonic.kubernetes.kubernetes;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

public class SearchersProducer
{
    @Singleton
    @Produces
    Searchers searchers( Informers informers )
    {
        singletonAssert(this, "createSearchers");
        return SearchersImpl.builder().
            configMap( new InformerSearcher<>( informers.configMapInformer() ) ).
            ingress( new InformerSearcher<>( informers.ingressInformer() ) ).
            namespace( new InformerSearcher<>( informers.namespaceInformer() ) ).
            pod( new InformerSearcher<>( informers.podInformer() ) ).
            xp7App( new InformerSearcher<>( informers.xp7AppInformer() ) ).
            xp7Config( new InformerSearcher<>( informers.xp7ConfigInformer() ) ).
            xp7Deployment( new InformerSearcher<>( informers.xp7DeploymentInformer() ) ).
            domain( new InformerSearcher<>( informers.domainInformer() ) ).
            event( new InformerSearcher<>( informers.eventInformer() ) ).
            build();
    }
}
