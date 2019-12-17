package com.enonic.ec.kubernetes.operator.info;

import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;

import com.enonic.ec.kubernetes.operator.common.Configuration;

public abstract class ResourceInfo<T extends HasMetadata, D extends Diff<T>>
    extends Configuration
{
    public abstract Optional<T> oldResource();

    public abstract Optional<T> newResource();

    @Value.Derived
    public T resource()
    {
        Preconditions.checkState( oldResource().isPresent() || newResource().isPresent(), "Either old or new resource have to be present" );
        return newResource().orElse( oldResource().orElse( null ) );
    }

    @Value.Derived
    public boolean resourceAdded()
    {
        return oldResource().isEmpty() && newResource().isPresent();
    }

    @Value.Derived
    public boolean resourceModified()
    {
        return oldResource().isPresent() && newResource().isPresent() && !oldResource().equals( newResource() );
    }

    @Value.Derived
    public boolean resourceDeleted()
    {
        return oldResource().isPresent() && newResource().isEmpty();
    }

    @Value.Derived
    public D diff()
    {
        return createDiff( oldResource(), newResource() );
    }

    @Value.Derived
    public OwnerReference ownerReference()
    {
        return createOwnerReference( resource() );
    }

    public OwnerReference createOwnerReference( HasMetadata owner )
    {
        return new OwnerReference( owner.getApiVersion(), true, true, owner.getKind(), owner.getMetadata().getName(),
                                   owner.getMetadata().getUid() );
    }

    protected abstract D createDiff( final Optional<T> oldResource, final Optional<T> newResource );
}
