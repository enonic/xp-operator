package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Optional;


import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.client.KubernetesClient;


public class CommandBuilderServiceAccount
    extends GenericBuilder<KubernetesClient, ServiceAccount>
{
    public CommandBuilderServiceAccount( final KubernetesClient client )
    {
        super( client );
    }

    @Override
    protected Optional<ServiceAccount> getOldResource( final String namespace, final String name )
    {
        return Optional.ofNullable( client().
            serviceAccounts().
            inNamespace( namespace ).
            withName( name ).
            get() );
    }

    @Override
    protected Runnable createOrReplaceCommand( final String namespace, final ServiceAccount resource )
    {
        return () -> client().
            serviceAccounts().
            inNamespace( namespace ).
            createOrReplace( resource );
    }

    @Override
    protected Runnable updateCommand( final String namespace, final ServiceAccount resource )
    {
        return () -> client().
            serviceAccounts().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            patch( resource );
    }

    @Override
    protected Runnable deleteCommand( final String namespace, final ServiceAccount resource )
    {
        return () -> client().
            serviceAccounts().
            inNamespace( namespace ).
            withName( resource.getMetadata().getName() ).
            delete();
    }

    @Override
    protected boolean equalsSpec( final ServiceAccount o, final ServiceAccount n )
    {
        return true;
    }
}
