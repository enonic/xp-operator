package com.enonic.ec.kubernetes.operator.kubectl.newapply;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.ec.kubernetes.operator.kubectl.newapply.base.KubeCommandResource;


@Value.Immutable
public abstract class KubeCmdV1alpha2Xp7VHosts
    extends KubeCommandResource<V1alpha2Xp7VHost>
{
    @Override
    protected Optional<V1alpha2Xp7VHost> fetch( final V1alpha2Xp7VHost resource )
    {
        return Optional.ofNullable( clients().getVHostClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void create( final V1alpha2Xp7VHost resource )
    {
        clients().getVHostClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            create( resource );
    }

    @Override
    protected void update( final V1alpha2Xp7VHost resource )
    {
        clients().getVHostClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            replace( resource );
    }

    @Override
    protected void delete( final V1alpha2Xp7VHost resource )
    {
        clients().getVHostClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }
}
