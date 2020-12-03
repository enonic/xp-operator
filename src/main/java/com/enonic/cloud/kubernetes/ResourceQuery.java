package com.enonic.cloud.kubernetes;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;

import static com.enonic.cloud.common.Configuration.cfgStr;

public class ResourceQuery<T extends HasMetadata>
{
    private Stream<T> stream;

    public ResourceQuery( Stream<T> stream )
    {
        this.stream = stream;
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

    public ResourceQuery<T> isEnonicManaged()
    {
        return this.hasLabel( cfgStr( "operator.charts.values.labelKeys.managed" ), "true" );
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

    public ResourceQuery<T> olderThen( Instant instant )
    {
        return filter( r -> Instant.parse( r.getMetadata().getCreationTimestamp() ).isBefore( instant ) );
    }

    public ResourceQuery<T> olderThen( long seconds )
    {
        return olderThen( Instant.now().minusSeconds( seconds ) );
    }

    public ResourceQuery<T> youngerThen( Instant instant )
    {
        return filter( r -> Instant.parse( r.getMetadata().getCreationTimestamp() ).isAfter( instant ) );
    }

    public ResourceQuery<T> youngerThen( long seconds )
    {
        return youngerThen( Instant.now().minusSeconds( seconds ) );
    }

    public Stream<T> stream()
    {
        return this.stream;
    }

    public ResourceQuery<T> sorted( Comparator<? super T> s )
    {
        this.stream = this.stream.sorted( s );
        return this;
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
