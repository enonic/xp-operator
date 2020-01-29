package com.enonic.ec.kubernetes.operator.kubectl.newapply.base;


import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.ec.kubernetes.operator.operators.clients.Clients;

public abstract class KubeCommandResource<T extends HasMetadata>
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

    protected abstract void update( T resource );

    protected abstract void delete( T resource );

    public KubeCommand apply()
    {
        if ( oldResource().isPresent() )
        {
            return ImmutableKubeCommand.builder().
                action( KubeCommandAction.UPDATE ).
                resource( maybeNamespacedResource() ).
                cmd( () -> {
                    update( maybeNamespacedResource() );
                    return null;
                } ).
                build();
        }
        else
        {
            return ImmutableKubeCommand.builder().
                action( KubeCommandAction.CREATE ).
                resource( maybeNamespacedResource() ).
                cmd( () -> {
                    create( maybeNamespacedResource() );
                    return null;
                } ).
                build();
        }
    }

    public Optional<KubeCommand> delete()
    {
        if ( oldResource().isPresent() )
        {
            return Optional.of( ImmutableKubeCommand.builder().
                action( KubeCommandAction.DELETE ).
                resource( maybeNamespacedResource() ).
                cmd( () -> {
                    delete( maybeNamespacedResource() );
                    return null;
                } ).
                build() );
        }
        return Optional.empty();
    }
}
