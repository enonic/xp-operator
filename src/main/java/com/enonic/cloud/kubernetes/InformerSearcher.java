package com.enonic.cloud.kubernetes;

import java.util.Optional;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

public class InformerSearcher<R extends HasMetadata>
{
    private final SharedIndexInformer<R> informer;

    public InformerSearcher( final SharedIndexInformer<R> informer )
    {
        this.informer = informer;
    }

    public Stream<R> getStream()
    {
        return informer.getIndexer().list().stream();
    }

    public Stream<R> get( @NotNull String namespace )
    {
        return getStream().filter( r -> namespace.equals( r.getMetadata().getNamespace() ) );
    }

    public Optional<R> get( String namespace, @NotNull String name )
    {
        return getStream().
            filter( r -> namespace == null || namespace.equals( r.getMetadata().getNamespace() ) ).
            filter( r -> name.equals( r.getMetadata().getName() ) ).
            findFirst();
    }

    public Optional<R> get( R resource )
    {
        return getStream().filter( r -> resource.getMetadata().getUid().equals( r.getMetadata().getUid() ) ).findFirst();
    }
}
