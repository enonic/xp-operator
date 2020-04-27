package com.enonic.cloud.operator.common.info;

import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;

public abstract class ResourceInfo<T extends HasMetadata, D extends Diff<T>>
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
    public String name()
    {
        return resource().getMetadata().getName();
    }

    @Value.Derived
    public boolean resourceBeingRestoredFromBackup()
    {
        if ( resource().getMetadata().getLabels() == null )
        {
            return false;
        }
        return diff().added() && resource().getMetadata().getLabels().containsKey( cfgStr( "operator.deployment.backup.label" ) );
    }

    @Value.Derived
    public D diff()
    {
        return createDiff( oldResource(), newResource() );
    }

    @Value.Derived
    public OwnerReference createResourceOwnerReference()
    {
        return createOwnerReference( resource() );
    }

    @SuppressWarnings("WeakerAccess")
    protected OwnerReference createOwnerReference( HasMetadata owner )
    {
        return new OwnerReference( owner.getApiVersion(), true, true, owner.getKind(), owner.getMetadata().getName(),
                                   owner.getMetadata().getUid() );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected abstract D createDiff( final Optional<T> oldResource, final Optional<T> newResource );
}
