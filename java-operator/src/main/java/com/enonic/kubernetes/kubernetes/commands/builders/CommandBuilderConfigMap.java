package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Objects;
import java.util.Optional;


import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;


public class CommandBuilderConfigMap
    extends GenericBuilder<KubernetesClient, ConfigMap>
{
    public CommandBuilderConfigMap( final KubernetesClient client )
    {
        super( client );
    }

    @Override
    protected Optional<ConfigMap> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            configMaps().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final ConfigMap resource )
    {
        return () -> client().
            configMaps().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final ConfigMap resource )
    {
        return () -> client().
            configMaps().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final ConfigMap resource )
    {
        return () -> client().
            configMaps().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final ConfigMap o, final ConfigMap n )
    {
        return Objects.equals( o.getData(), n.getData() );
    }
}
