package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.kubernetes.client.v1alpha2.xp7vhost.Xp7VHostClient;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;


@Value.Immutable
public abstract class CommandBuilderV1Alpha2Xp7VHost
    extends GenericBuilder<Xp7VHostClient, Xp7VHost>
{
    @Override
    protected Optional<Xp7VHost> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().crdClient().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Xp7VHost resource )
    {
        return () -> client().crdClient().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Xp7VHost resource )
    {
        return () -> client().crdClient().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Xp7VHost resource )
    {
        return () -> client().crdClient().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Xp7VHost o, final Xp7VHost n )
    {
        return Objects.equals( o.getXp7VHostSpec(), n.getXp7VHostSpec() );
    }
}
