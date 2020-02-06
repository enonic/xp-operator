package com.enonic.ec.kubernetes.operator.kubectl.base;


import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;

public abstract class KubeCommandBuilder<T extends HasMetadata>
{
    public abstract Clients clients();

    public abstract Optional<String> namespace();

    public abstract T resource();

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
            if ( equalsResourcesWithMetadata( oldResource().get(), maybeNamespacedResource() ) )
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
        if ( !equalsMetadata( o.getMetadata(), n.getMetadata() ) )
        {
            return false;
        }
        return equalsSpec( o, n );
    }

    private boolean equalsMetadata( ObjectMeta o, ObjectMeta n )
    {
        return Objects.equals( o.getName(), n.getName() ) && Objects.equals( o.getNamespace(), n.getNamespace() ) &&
            Objects.equals( o.getLabels(), n.getLabels() ) && Objects.equals( o.getAnnotations(), n.getAnnotations() );
    }

    protected boolean equalsSpec( final T o, final T n )
    {
        return Objects.equals( o, n );
    }

}
