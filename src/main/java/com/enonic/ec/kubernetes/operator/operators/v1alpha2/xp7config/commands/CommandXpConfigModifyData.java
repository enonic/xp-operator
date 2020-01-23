package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.commands;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.ImmutableV1alpha2Xp7ConfigSpec;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyXp7Config;
import com.enonic.ec.kubernetes.operator.kubectl.delete.ImmutableCommandDeleteXp7Config;
import com.enonic.ec.kubernetes.operator.operators.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.ResourceInfoNamespaced;

public abstract class CommandXpConfigModifyData
    extends Configuration
    implements CombinedCommandBuilder
{
    public abstract Clients clients();

    public abstract Caches caches();

    public abstract ResourceInfoNamespaced info();

    public abstract String name();

    public abstract String file();

    public abstract String nodeGroup();

    @Value.Derived
    protected Optional<V1alpha2Xp7Config> xpConfigResource()
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
                clients(clients()).
                namespace( info().deploymentInfo().namespaceName() ).
                name( name() ).
                build() );
        }
        else
        {
            // Apply the config
            commandBuilder.addCommand( ImmutableCommandApplyXp7Config.builder().
                clients(clients()).
                canSkipOwnerReference( true ). // TODO: Look at this
                namespace( info().deploymentInfo().namespaceName() ).
                name( name() ).
                spec( ImmutableV1alpha2Xp7ConfigSpec.builder().
                    file( file() ).
                    data( data ).
                    nodeGroup( nodeGroup() ).
                    build() ).
                build() );
        }
    }

    protected abstract void setData( final StringBuilder sb );
}
