package com.enonic.ec.kubernetes.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.kubectl.base.KubeCommandResource;


@Value.Immutable
public abstract class KubeCmdV1alpha2Xp7Configs
    extends KubeCommandResource<V1alpha2Xp7Config>
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
    protected void create( final V1alpha2Xp7Config resource )
    {
        clients().getConfigClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            create( resource );
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
    protected boolean compareSpec( final V1alpha2Xp7Config o, final V1alpha2Xp7Config n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
