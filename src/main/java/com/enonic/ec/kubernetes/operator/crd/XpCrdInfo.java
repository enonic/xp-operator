package com.enonic.ec.kubernetes.operator.crd;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;

import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;

@Value.Immutable
public abstract class XpCrdInfo
{
    protected abstract Optional<XpDeploymentCache> cache();

    protected abstract Optional<HasMetadata> oldResource();

    protected abstract Optional<HasMetadata> newResource();

    @Value.Derived
    public String namespace()
    {
        return oldResource().map( m -> m.getMetadata().getNamespace() ).orElse(
            newResource().map( m -> m.getMetadata().getNamespace() ).orElse( null ) );
    }

    @Value.Derived
    public XpDeploymentResource xpDeployment()
    {
        Optional<XpDeploymentResource> res = cache().map( c -> c.getByName( namespace() ).orElse( null ) );
        if ( !res.isPresent() )
        {
            HasMetadata obj = newResource().orElse( oldResource().orElse( null ) );
            if ( obj instanceof XpDeploymentResource )
            {
                return (XpDeploymentResource) obj;
            }
        }
        return res.orElse( null );
    }

    @Value.Check
    protected void check()
    {
        if ( oldResource().isPresent() && newResource().isPresent() )
        {

            Preconditions.checkState(
                Objects.equals( oldResource().get().getMetadata().getName(), newResource().get().getMetadata().getName() ),
                "Resource names must be the same" );
            Preconditions.checkState(
                Objects.equals( oldResource().get().getMetadata().getNamespace(), newResource().get().getMetadata().getNamespace() ),
                "Resource namespace must be the same" );
        }
        Preconditions.checkState( xpDeployment() != null, "XpDeployment not found" );
    }

    @Value.Derived
    public OwnerReference xpDeploymentOwnerReference()
    {
        return createOwnerReference( xpDeployment() );
    }

    @Value.Derived
    public Optional<OwnerReference> resourceOwnerReference()
    {
        return newResource().map( this::createOwnerReference );
    }

    public OwnerReference createOwnerReference( final HasMetadata owner )
    {
        return new OwnerReference( owner.getApiVersion(), true, true, owner.getKind(), owner.getMetadata().getName(),
                                   owner.getMetadata().getUid() );
    }
}
