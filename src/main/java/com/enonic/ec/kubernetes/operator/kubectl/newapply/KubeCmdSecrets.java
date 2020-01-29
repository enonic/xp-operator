package com.enonic.ec.kubernetes.operator.kubectl.newapply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Secret;

import com.enonic.ec.kubernetes.operator.kubectl.newapply.base.KubeCommandResource;


@Value.Immutable
public abstract class KubeCmdSecrets
    extends KubeCommandResource<Secret>
{
    @Override
    protected Optional<Secret> fetch( final Secret resource )
    {
        return Optional.ofNullable( clients().getDefaultClient().secrets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void create( final Secret resource )
    {
        clients().getDefaultClient().secrets().
            inNamespace( resource.getMetadata().getNamespace() ).
            create( resource );
    }

    @Override
    protected void update( final Secret resource )
    {
        clients().getDefaultClient().secrets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            replace( resource );
    }

    @Override
    protected void delete( final Secret resource )
    {
        clients().getDefaultClient().secrets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }
}
