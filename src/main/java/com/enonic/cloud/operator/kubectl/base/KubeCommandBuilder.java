package com.enonic.cloud.operator.kubectl.base;


import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.operator.operators.common.clients.Clients;

public abstract class KubeCommandBuilder<T extends HasMetadata>
{
    public abstract Clients clients();

    public abstract Optional<String> namespace();

    public abstract T resource();

    public abstract KubeCommandOptions options();

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

    protected abstract void createOrReplace( T resource );

    protected abstract void patch( T resource );

    protected abstract void delete( T resource );

    public Optional<KubeCommand> apply()
    {
        KubeCommandAction action;

        // If old resource is present
        if ( oldResource().isPresent() )
        {
            // If neverOverwrite flag is set, ignore
            if ( options().neverOverwrite().orElse( false ) )
            {
                action = KubeCommandAction.NONE;
            }
            // If always override is set replace
            else if ( options().alwaysOverwrite().orElse( false ) )
            {
                action = KubeCommandAction.REPLACE;
            }
            // Else just update
            else
            {
                action = KubeCommandAction.UPDATE;
            }
        }
        // There is no old resource
        else
        {
            action = KubeCommandAction.CREATE;
        }

        // If action is update but resources are identical
        if ( action == KubeCommandAction.UPDATE && equalsResourcesWithMetadata( oldResource().get(), maybeNamespacedResource() ) )
        {
            // If we are not forcing an update
            if ( !options().alwaysUpdate().orElse( false ) )
            {
                action = KubeCommandAction.NONE;
            }
        }

        switch ( action )
        {
            case CREATE:
            case REPLACE:
                return Optional.of( ImmutableKubeCommand.builder().
                    action( action ).
                    resource( maybeNamespacedResource() ).
                    cmd( () -> createOrReplace( maybeNamespacedResource() ) ).
                    build() );
            case UPDATE:
                return Optional.of( ImmutableKubeCommand.builder().
                    action( action ).
                    resource( maybeNamespacedResource() ).
                    cmd( () -> patch( maybeNamespacedResource() ) ).
                    build() );
            default:
                return Optional.empty();
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

    @SuppressWarnings("WeakerAccess")
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

    private <R> R getOrDefault( R value, Supplier<R> sup )
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
