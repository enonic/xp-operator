package com.enonic.ec.kubernetes.operator.commands.config;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyXp7Config;
import com.enonic.ec.kubernetes.kubectl.delete.ImmutableCommandDeleteXp7Config;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigCache;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClient;
import com.enonic.ec.kubernetes.operator.crd.config.spec.ImmutableSpec;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

public abstract class CommandXpConfigModifyData
    extends Configuration
    implements CombinedCommandBuilder
{
    public abstract XpConfigClient xpConfigClient();

    public abstract XpConfigCache xpConfigCache();

    public abstract ResourceInfoNamespaced info();

    public abstract String name();

    public abstract String file();

    public abstract String node();

    @Value.Derived
    protected Optional<XpConfigResource> xpConfigResource()
    {
        return xpConfigCache().get( info().namespace(), name() );
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
                namespace( info().namespace() ).
                name( name() ).
                build() );
        }
        else
        {
            // Apply the config
            commandBuilder.addCommand( ImmutableCommandApplyXp7Config.builder().
                client( xpConfigClient() ).
                canSkipOwnerReference( true ). // TODO: Look at this
                namespace( info().namespace() ).
                name( name() ).
                spec( ImmutableSpec.builder().
                    file( file() ).
                    data( data ).
                    node( node() ).
                    build() ).
                build() );
        }
    }

    protected abstract void setData( final StringBuilder sb );
}
