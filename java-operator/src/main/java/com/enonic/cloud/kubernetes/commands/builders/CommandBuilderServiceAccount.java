package com.enonic.cloud.kubernetes.commands.builders;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.client.KubernetesClient;


@Value.Immutable
public abstract class CommandBuilderServiceAccount
    extends GenericBuilder<KubernetesClient, ServiceAccount>
{
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
