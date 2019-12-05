package com.enonic.ec.kubernetes.operator.commands.config;

import java.util.Optional;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyXp7Config;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClient;
import com.enonic.ec.kubernetes.operator.crd.config.spec.ImmutableSpec;

public abstract class XpConfigModifyData
    extends Configuration
    implements CombinedCommandBuilder
{
    public abstract XpConfigClient client();

    public abstract String namespace();

    public abstract String name();

    public abstract String file();

    public abstract Optional<XpConfigResource> xpConfigResource();

    public abstract Optional<String> node();

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

        commandBuilder.addCommand( ImmutableCommandApplyXp7Config.builder().
            client( client() ).
            canSkipOwnerReference( true ).
            namespace( namespace() ).
            name( name() ).
            spec( ImmutableSpec.builder().
                file( file() ).
                data( data ).
                node( node().orElse( null ) ).
                build() ).
            build() );
    }

    protected abstract void setData( final StringBuilder sb );
}
