package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;


@Value.Immutable
public abstract class CommandBuilderIngress
    extends GenericBuilder<KubernetesClient, Ingress>
{
    @Override
    protected Optional<Ingress> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            network().
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
            ingresses().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final Ingress resource )
    {
        return () -> client().
            network().
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
