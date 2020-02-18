package com.enonic.cloud.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdV1alpha2Xp7Configs
    extends KubeCommandBuilder<V1alpha2Xp7Config>
{
    @Override
    protected Optional<V1alpha2Xp7Config> fetch( final V1alpha2Xp7Config resource )
    {
        return Optional.ofNullable( clients().getConfigClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void createOrReplace( final V1alpha2Xp7Config resource )
    {
        clients().getConfigClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            createOrReplace( resource );
    }

    @Override
    protected void patch( final V1alpha2Xp7Config resource )
    {
        clients().getConfigClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final V1alpha2Xp7Config resource )
    {
        clients().getConfigClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final V1alpha2Xp7Config o, final V1alpha2Xp7Config n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}