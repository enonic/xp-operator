package com.enonic.ec.kubernetes.operator.commands.config;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyXp7Config;
import com.enonic.ec.kubernetes.kubectl.delete.ImmutableCommandDeleteXp7Config;
import com.enonic.ec.kubernetes.operator.crd.XpCrdInfo;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigCache;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClient;
import com.enonic.ec.kubernetes.operator.crd.config.spec.ImmutableSpec;

public abstract class XpConfigModifyData
    extends Configuration
    implements CombinedCommandBuilder
{
    public abstract XpConfigClient client();

    public abstract XpConfigCache xpConfigCache();

    public abstract XpCrdInfo info();

    public abstract String name();

    public abstract String file();

    public abstract Optional<String> node();

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
        String data = sb.toString();

        if ( xpConfigResource().isPresent() && xpConfigResource().get().getSpec().data().equals( data ) )
        {
            return;
        }

        if ( data.equals( "" ) )
        {
            commandBuilder.addCommand( ImmutableCommandDeleteXp7Config.builder().
                client( client() ).
                namespace( info().namespace() ).
                name( name() ).
                build() );
        }
        else
        {
            commandBuilder.addCommand( ImmutableCommandApplyXp7Config.builder().
                client( client() ).
                canSkipOwnerReference( true ).
                namespace( info().namespace() ).
                name( name() ).
                spec( ImmutableSpec.builder().
                    file( file() ).
                    data( data ).
                    node( node().orElse( null ) ).
                    build() ).
                build() );
        }
    }

    protected abstract void setData( final StringBuilder sb );
}
