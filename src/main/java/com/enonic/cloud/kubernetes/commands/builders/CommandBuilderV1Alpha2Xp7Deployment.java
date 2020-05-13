package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;


@Value.Immutable
public abstract class CommandBuilderV1Alpha2Xp7Deployment
    extends GenericBuilder<CrdClient, V1alpha2Xp7Deployment>
{
    @Override
    protected Optional<V1alpha2Xp7Deployment> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            xp7Deployments().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final V1alpha2Xp7Deployment resource )
    {
        return () -> client().
            xp7Deployments().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final V1alpha2Xp7Deployment resource )
    {
        return () -> client().
            xp7Deployments().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final V1alpha2Xp7Deployment resource )
    {
        return () -> client().
            xp7Deployments().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final V1alpha2Xp7Deployment o, final V1alpha2Xp7Deployment n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
