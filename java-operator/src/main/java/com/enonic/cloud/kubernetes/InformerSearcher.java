package com.enonic.cloud.kubernetes;

import java.util.stream.Stream;

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

    public Stream<R> stream()
    {
        return informer.getIndexer().list().stream();
    }
}
