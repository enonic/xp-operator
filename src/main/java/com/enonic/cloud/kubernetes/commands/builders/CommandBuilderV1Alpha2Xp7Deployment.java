package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentClient;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;


@Value.Immutable
public abstract class CommandBuilderV1Alpha2Xp7Deployment
    extends GenericBuilder<Xp7DeploymentClient, Xp7Deployment>
{
    @Override
    protected Optional<Xp7Deployment> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().crdClient().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Xp7Deployment resource )
    {
        return () -> client().crdClient().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Xp7Deployment resource )
    {
        return () -> client().crdClient().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Xp7Deployment resource )
    {
        return () -> client().crdClient().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Xp7Deployment o, final Xp7Deployment n )
    {
        return Objects.equals( o.getXp7DeploymentSpec(), n.getXp7DeploymentSpec() );
    }
}
