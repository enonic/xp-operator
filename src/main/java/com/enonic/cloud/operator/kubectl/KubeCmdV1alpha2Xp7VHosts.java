package com.enonic.cloud.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdV1alpha2Xp7VHosts
    extends KubeCommandBuilder<V1alpha2Xp7VHost>
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
    protected void createOrReplace( final V1alpha2Xp7VHost resource )
    {
        clients().getVHostClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            createOrReplace( resource );
    }

    @Override
    protected void patch( final V1alpha2Xp7VHost resource )
    {
        clients().getVHostClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final V1alpha2Xp7VHost resource )
    {
        clients().getVHostClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final V1alpha2Xp7VHost o, final V1alpha2Xp7VHost n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
