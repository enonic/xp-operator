package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;


import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;


public class CommandBuilderService
    extends GenericBuilder<KubernetesClient, Service>
{
    public CommandBuilderService( final KubernetesClient client )
    {
        super( client );
    }

    @Override
    protected Optional<Service> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            services().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Service resource )
    {
        return () -> client().
            services().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Service resource )
    {
        return () -> client().
            services().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Service resource )
    {
        return () -> client().
            services().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Service o, final Service n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
