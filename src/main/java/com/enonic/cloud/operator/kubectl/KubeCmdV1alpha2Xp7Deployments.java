package com.enonic.cloud.operator.kubectl;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.kubectl.base.KubeCommandBuilder;


@Value.Immutable
public abstract class KubeCmdV1alpha2Xp7Deployments
    extends KubeCommandBuilder<V1alpha2Xp7Deployment>
{
    @Override
    protected Optional<V1alpha2Xp7Deployment> fetch( final V1alpha2Xp7Deployment resource )
    {
        return Optional.ofNullable( clients().getDeploymentClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            get() );
    }

    @Override
    protected void createOrReplace( final V1alpha2Xp7Deployment resource )
    {
        clients().getDeploymentClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            createOrReplace( resource );
    }

    @Override
    protected void patch( final V1alpha2Xp7Deployment resource )
    {
        clients().getDeploymentClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected void delete( final V1alpha2Xp7Deployment resource )
    {
        clients().getDeploymentClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final V1alpha2Xp7Deployment o, final V1alpha2Xp7Deployment n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
