package com.enonic.cloud.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdV1alpha1Xp7Apps
    extends KubeCommandBuilder<V1alpha1Xp7App>
{
    @Override
    protected Optional<V1alpha1Xp7App> fetch( final V1alpha1Xp7App resource )
    {
        return Optional.ofNullable( clients().getAppClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void createOrReplace( final V1alpha1Xp7App resource )
    {
        clients().getAppClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            createOrReplace( resource );
    }

    @Override
    protected void patch( final V1alpha1Xp7App resource )
    {
        clients().getAppClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final V1alpha1Xp7App resource )
    {
        clients().getAppClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final V1alpha1Xp7App o, final V1alpha1Xp7App n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}