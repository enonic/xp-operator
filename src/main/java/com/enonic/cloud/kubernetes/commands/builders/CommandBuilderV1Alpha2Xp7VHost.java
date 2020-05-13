package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;


@Value.Immutable
public abstract class CommandBuilderV1Alpha2Xp7VHost
    extends GenericBuilder<CrdClient, V1alpha2Xp7VHost>
{
    @Override
    protected Optional<V1alpha2Xp7VHost> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            xp7VHosts().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final V1alpha2Xp7VHost resource )
    {
        return () -> client().
            xp7VHosts().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final V1alpha2Xp7VHost resource )
    {
        return () -> client().
            xp7VHosts().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final V1alpha2Xp7VHost resource )
    {
        return () -> client().
            xp7VHosts().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }
    
    @Override
    protected boolean equalsSpec( final V1alpha2Xp7VHost o, final V1alpha2Xp7VHost n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
