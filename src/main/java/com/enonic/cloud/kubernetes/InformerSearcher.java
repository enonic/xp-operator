package com.enonic.cloud.kubernetes;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

public class InformerSearcher<R extends HasMetadata>
{
    private final SharedIndexInformer<R> informer;

    protected InformerSearcher()
    {
        informer = null;
    }

    public InformerSearcher( final SharedIndexInformer<R> informer )
    {
        this.informer = informer;
    }

    public ResourceQuery<R> query()
    {
        return new ResourceQuery<>( informer.getIndexer().list().stream() );
    }
}
