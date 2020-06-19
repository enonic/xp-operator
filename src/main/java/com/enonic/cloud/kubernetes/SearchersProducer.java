package com.enonic.cloud.kubernetes;

import javax.inject.Singleton;
import javax.ws.rs.Produces;

public class SearchersProducer
{
    @Singleton
    @Produces
    Searchers searchers( Informers informers )
    {
        return SearchersImpl.builder().
            configMap( new InformerSearcher<>( informers.configMapInformer() ) ).
            ingress( new InformerSearcher<>( informers.ingressInformer() ) ).
            namespace( new InformerSearcher<>( informers.namespaceInformer() ) ).
            pod( new InformerSearcher<>( informers.podInformer() ) ).
            xp7App( new InformerSearcher<>( informers.xp7AppInformer() ) ).
            xp7Config( new InformerSearcher<>( informers.xp7ConfigInformer() ) ).
            xp7Deployment( new InformerSearcher<>( informers.xp7DeploymentInformer() ) ).
            xp7VHost( new InformerSearcher<>( informers.xp7VHostInformer() ) ).
            build();
    }
}
