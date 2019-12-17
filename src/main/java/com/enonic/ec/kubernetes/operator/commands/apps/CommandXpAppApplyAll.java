package com.enonic.ec.kubernetes.operator.commands.apps;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResource;
import com.enonic.ec.kubernetes.operator.crd.xp7app.client.Xp7AppCache;
import com.enonic.ec.kubernetes.operator.info.xp7app.DiffXp7App;
import com.enonic.ec.kubernetes.operator.crd.xp7config.client.Xp7ConfigCache;
import com.enonic.ec.kubernetes.operator.crd.xp7config.client.Xp7ConfigClient;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

@Value.Immutable
public abstract class CommandXpAppApplyAll
    extends Configuration
    implements CombinedCommandBuilder
{
    public abstract Xp7ConfigClient xpConfigClient();

    public abstract Xp7ConfigCache xpConfigCache();

    protected abstract Xp7AppCache xpAppCache();

    protected abstract ResourceInfoNamespaced<Xp7AppResource, DiffXp7App> info();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Get all apps in namespace
        List<Xp7AppResource> allApps = xpAppCache().getByNamespace( info().deploymentInfo().namespaceName() ).collect( Collectors.toList() );

        // Apply them to the config file
        ImmutableCommandXpAppApply.builder().
            xpConfigClient( xpConfigClient() ).
            xpConfigCache( xpConfigCache() ).
            info( info() ).
            node( cfgStr( "operator.deployment.xp.allNodes" ) ).
            name( cfgStrFmt( "operator.config.xp.deploy.name", cfgStr( "operator.deployment.xp.allNodes" ) ) ).
            file( cfgStr( "operator.config.xp.deploy.file" ) ).
            xpAppResources( allApps ).
            build().
            addCommands( commandBuilder );
    }
}
