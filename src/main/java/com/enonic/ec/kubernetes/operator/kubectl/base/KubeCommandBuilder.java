package com.enonic.ec.kubernetes.operator.kubectl.base;


import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;

public abstract class KubeCommandBuilder<T extends HasMetadata>
{
    public abstract Clients clients();

    public abstract Optional<String> namespace();

    public abstract T resource();

    public abstract boolean neverOverwrite();

    @Value.Derived
    protected T maybeNamespacedResource()
    {
        T res = resource();
        namespace().ifPresent( ns -> res.getMetadata().setNamespace( ns ) );
        return res;
    }

    @Value.Derived
    protected Optional<T> oldResource()
    {
        return fetch( maybeNamespacedResource() );
    }

    protected abstract Optional<T> fetch( T resource );

    protected abstract void create( T resource );

    protected abstract void patch( T resource );

    protected abstract void delete( T resource );

    public Optional<KubeCommand> apply()
    {
        if ( oldResource().isPresent() )
        {
            if ( neverOverwrite() || equalsResourcesWithMetadata( oldResource().get(), maybeNamespacedResource() ) )
            {
                return Optional.empty();
            }
            else
            {
                return Optional.of( ImmutableKubeCommand.builder().
                    action( KubeCommandAction.UPDATE ).
                    resource( maybeNamespacedResource() ).
                    cmd( () -> patch( maybeNamespacedResource() ) ).
                    build() );
            }
        }
        else
        {
            return Optional.of( ImmutableKubeCommand.builder().
                action( KubeCommandAction.CREATE ).
                resource( maybeNamespacedResource() ).
                cmd( () -> create( maybeNamespacedResource() ) ).
                build() );
        }
    }

    public Optional<KubeCommand> delete()
    {
        if ( oldResource().isPresent() )
        {
            return Optional.of( ImmutableKubeCommand.builder().
                action( KubeCommandAction.DELETE ).
                resource( maybeNamespacedResource() ).
                cmd( () -> delete( maybeNamespacedResource() ) ).
                build() );
        }
        return Optional.empty();
    }

    private boolean equalsResourcesWithMetadata( T o, T n )
    {
        Preconditions.checkState( Objects.equals( o.getMetadata().getName(), n.getMetadata().getName() ) );
        Preconditions.checkState( Objects.equals( o.getMetadata().getNamespace(), n.getMetadata().getNamespace() ) );
        if ( !equalLabels( getOrDefault( o.getMetadata().getLabels() ), getOrDefault( n.getMetadata().getLabels() ) ) )
        {
            return false;
        }
        if ( !equalAnnotations( getOrDefault( o.getMetadata().getAnnotations() ), getOrDefault( n.getMetadata().getAnnotations() ) ) )
        {
            return false;
        }
        return equalsSpec( o, n );
    }

    protected boolean equalLabels( Map<String, String> o, Map<String, String> n )
    {
        return Objects.equals( o, n );
    }

    protected boolean equalAnnotations( final Map<String, String> o, final Map<String, String> n )
    {
        return Objects.equals( o, n );
    }

    protected boolean equalsSpec( final T o, final T n )
    {
        return Objects.equals( o, n );
    }

    private <T> T getOrDefault( T value, Supplier<T> sup )
    {
        if ( value == null )
        {
            return sup.get();
        }
        return value;
    }

    private Map<String, String> getOrDefault( Map<String, String> m )
    {
        return getOrDefault( m, Collections::emptyMap );
    }
}
