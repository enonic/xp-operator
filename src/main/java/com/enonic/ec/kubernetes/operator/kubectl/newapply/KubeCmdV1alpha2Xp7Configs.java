package com.enonic.ec.kubernetes.operator.kubectl.newapply;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.kubectl.newapply.base.KubeCommandResource;


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
    protected void update( final V1alpha2Xp7Config resource )
    {
        clients().getConfigClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            replace( resource );
    }

    @Override
    protected void delete( final V1alpha2Xp7Config resource )
    {
        clients().getConfigClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }
}
