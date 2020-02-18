package com.enonic.cloud.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Secret;

import com.enonic.cloud.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdSecrets
    extends KubeCommandBuilder<Secret>
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
    protected void createOrReplace( final Secret resource )
    {
        clients().getDefaultClient().secrets().
            inNamespace( resource.getMetadata().getNamespace() ).
            createOrReplace( resource );
    }

    @Override
    protected void patch( final Secret resource )
    {
        clients().getDefaultClient().secrets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final Secret resource )
    {
        clients().getDefaultClient().secrets().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Secret o, final Secret n )
    {
        return Objects.equals( o.getData(), n.getData() );
    }
}
