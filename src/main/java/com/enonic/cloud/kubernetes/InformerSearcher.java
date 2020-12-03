package com.enonic.cloud.kubernetes;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

    public List<R> list()
    {
        return informer.getIndexer().list();
    }

    public Stream<R> stream()
    {
        return list().stream();
    }

    @Deprecated
    public ResourceQuery<R> query()
    {
        return new ResourceQuery<>( informer.getIndexer().list().stream() );
    }
}
