package com.enonic.kubernetes.kubernetes.commands.builders;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Objects;
import java.util.Optional;


public class CommandBuilderIngress
    extends GenericBuilder<KubernetesClient, Ingress>
{
    public CommandBuilderIngress( final KubernetesClient client )
    {
        super( client );
    }

    @Override
    protected Optional<Ingress> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            network().
            v1().
            ingresses().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final Ingress resource )
    {
        return () -> client().
            network().
            v1().
            ingresses().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Ingress resource )
    {
        return () -> client().
            network().
            v1().
            ingresses().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final Ingress resource )
    {
        return () -> client().
            network().
            v1().
            ingresses().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final Ingress o, final Ingress n )
    {
        return Objects.equals( o.getSpec(), n.getSpec() );
    }
}
