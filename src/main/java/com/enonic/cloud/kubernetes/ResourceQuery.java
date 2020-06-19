package com.enonic.cloud.kubernetes;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

public class ResourceQuery<T extends HasMetadata>
{
    private Stream<T> stream;

    private ResourceQuery( Stream<T> stream )
    {
        this.stream = stream;
    }

    public ResourceQuery( SharedIndexInformer<T> informer )
    {
        this( informer.getIndexer().list().stream() );
    }

    public static <R extends HasMetadata> ResourceQuery<R> resourceQuery( R resource )
    {
        return new ResourceQuery<>( Stream.of( resource ) );
    }

    public ResourceQuery<T> filter( Predicate<T> p )
    {
        this.stream = this.stream.filter( p );
        return this;
    }

    public ResourceQuery<T> inNamespace( String namespace )
    {
        return filter( r -> Objects.equals( r.getMetadata().getNamespace(), namespace ) );
    }

    @SuppressWarnings("unused")
    public ResourceQuery<T> withName( String name )
    {
        return filter( r -> Objects.equals( r.getMetadata().getName(), name ) );
    }

    @SuppressWarnings("WeakerAccess")
    public ResourceQuery<T> hasAnnotations()
    {
        return filter( r -> r.getMetadata().getAnnotations() != null && !r.getMetadata().getAnnotations().isEmpty() );
    }

    @SuppressWarnings("unused")
    public ResourceQuery<T> hasAnnotation( String key )
    {
        return hasAnnotations().filter( r -> r.getMetadata().getAnnotations().containsKey( key ) );
    }

    public ResourceQuery<T> hasAnnotation( String key, String value )
    {
        return hasAnnotations().filter( r -> Objects.equals( r.getMetadata().getAnnotations().get( key ), value ) );
    }

    @SuppressWarnings("WeakerAccess")
    public ResourceQuery<T> hasLabels()
    {
        return filter( r -> r.getMetadata().getLabels() != null && !r.getMetadata().getLabels().isEmpty() );
    }

    public ResourceQuery<T> hasLabel( String key )
    {
        return hasLabels().filter( r -> r.getMetadata().getLabels().containsKey( key ) );
    }

    @SuppressWarnings("unused")
    public ResourceQuery<T> hasLabel( String key, String value )
    {
        return hasLabels().filter( r -> Objects.equals( r.getMetadata().getLabels().get( key ), value ) );
    }

    public ResourceQuery<T> hasFinalizer( String key )
    {
        return filter( r -> r.getMetadata().getFinalizers().contains( key ) );
    }

    public ResourceQuery<T> hasNotBeenDeleted()
    {
        return filter( r -> r.getMetadata().getDeletionTimestamp() == null );
    }

    public ResourceQuery<T> hasBeenDeleted()
    {
        return filter( r -> r.getMetadata().getDeletionTimestamp() != null );
    }

    public ResourceQuery<T> olderThen( long seconds )
    {
        return filter(
            r -> Duration.between( Instant.parse( r.getMetadata().getCreationTimestamp() ), Instant.now() ).getSeconds() > seconds );
    }

    public ResourceQuery<T> youngerThen( long seconds )
    {
        return filter(
            r -> Duration.between( Instant.parse( r.getMetadata().getCreationTimestamp() ), Instant.now() ).getSeconds() < seconds );
    }

    public Stream<T> stream()
    {
        return this.stream;
    }

    public void forEach( Consumer<T> consumer )
    {
        this.stream.forEach( consumer );
    }

    public List<T> list()
    {
        return this.stream.collect( Collectors.toList() );
    }

    public Set<T> set()
    {
        return this.stream.collect( Collectors.toSet() );
    }

    public Optional<T> get()
    {
        return stream().findFirst();
    }
}
