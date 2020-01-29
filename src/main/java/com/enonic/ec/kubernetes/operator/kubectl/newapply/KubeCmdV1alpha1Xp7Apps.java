package com.enonic.ec.kubernetes.operator.kubectl.newapply;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.kubectl.newapply.base.KubeCommandResource;


@Value.Immutable
public abstract class KubeCmdV1alpha1Xp7Apps
    extends KubeCommandResource<V1alpha1Xp7App>
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
    protected void create( final V1alpha1Xp7App resource )
    {
        clients().getAppClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            create( resource );
    }

    @Override
    protected void update( final V1alpha1Xp7App resource )
    {
        clients().getAppClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            replace( resource );
    }

    @Override
    protected void delete( final V1alpha1Xp7App resource )
    {
        clients().getAppClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }
}
