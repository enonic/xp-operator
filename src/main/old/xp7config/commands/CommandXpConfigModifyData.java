package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.commands;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyXp7Config;
import com.enonic.ec.kubernetes.operator.kubectl.delete.ImmutableCommandDeleteXp7Config;
import com.enonic.ec.kubernetes.operator.operators.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client.Xp7ConfigCache;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client.Xp7ConfigClient;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.spec.ImmutableXp7ConfigSpec;

public abstract class CommandXpConfigModifyData
    extends Configuration
    implements CombinedCommandBuilder
{
    public abstract Clients clients();

    public abstract Caches caches();

    public abstract ResourceInfoNamespaced info();

    public abstract String name();

    public abstract String file();

    public abstract String node();

    @Value.Derived
    protected Optional<Xp7ConfigResource> xpConfigResource()
    {
        return caches().getConfigCache().get( info().deploymentInfo().namespaceName(), name() );
    }

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        StringBuilder sb = new StringBuilder();
        setData( sb );
        String data = sb.toString().trim();

        if ( xpConfigResource().isPresent() && xpConfigResource().get().getSpec().data().equals( data ) )
        {
            // There is no change in this config
            return;
        }

        if ( data.equals( "" ) )
        {
            // The config is empty, delete the config
            commandBuilder.addCommand( ImmutableCommandDeleteXp7Config.builder().
                client( xpConfigClient() ).
                namespace( info().deploymentInfo().namespaceName() ).
                name( name() ).
                build() );
        }
        else
        {
            // Apply the config
            commandBuilder.addCommand( ImmutableCommandApplyXp7Config.builder().
                client( xpConfigClient() ).
                canSkipOwnerReference( true ). // TODO: Look at this
                namespace( info().deploymentInfo().namespaceName() ).
                name( name() ).
                spec( ImmutableXp7ConfigSpec.builder().
                    file( file() ).
                    data( data ).
                    node( node() ).
                    build() ).
                build() );
        }
    }

    protected abstract void setData( final StringBuilder sb );
}
