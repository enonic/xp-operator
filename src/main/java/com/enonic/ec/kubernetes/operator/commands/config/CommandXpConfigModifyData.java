package com.enonic.ec.kubernetes.operator.commands.config;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyXp7Config;
import com.enonic.ec.kubernetes.kubectl.delete.ImmutableCommandDeleteXp7Config;
import com.enonic.ec.kubernetes.operator.crd.xp7config.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.crd.xp7config.client.Xp7ConfigCache;
import com.enonic.ec.kubernetes.operator.crd.xp7config.client.Xp7ConfigClient;
import com.enonic.ec.kubernetes.operator.crd.xp7config.spec.ImmutableXp7ConfigSpec;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

public abstract class CommandXpConfigModifyData
    extends Configuration
    implements CombinedCommandBuilder
{
    public abstract Xp7ConfigClient xpConfigClient();

    public abstract Xp7ConfigCache xpConfigCache();

    public abstract ResourceInfoNamespaced info();

    public abstract String name();

    public abstract String file();

    public abstract String node();

    @Value.Derived
    protected Optional<Xp7ConfigResource> xpConfigResource()
    {
        return xpConfigCache().get( info().deploymentInfo().namespaceName(), name() );
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
