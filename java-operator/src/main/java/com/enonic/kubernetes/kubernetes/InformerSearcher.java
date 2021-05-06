package com.enonic.kubernetes.kubernetes;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

    @SafeVarargs
    public final Stream<R> filter( Predicate<? super R>... predicates )
    {
        Stream<R> s = stream();
        for (Predicate<? super R> p : predicates) {
            s = s.filter( p );
        }
        return s;
    }

    @SafeVarargs
    public final Optional<R> find( Predicate<? super R>... predicates )
    {
        return filter( predicates ).findFirst();
    }

    @SafeVarargs
    public final boolean match( Predicate<? super R>... predicates )
    {
        return find( predicates ).isPresent();
    }
}
