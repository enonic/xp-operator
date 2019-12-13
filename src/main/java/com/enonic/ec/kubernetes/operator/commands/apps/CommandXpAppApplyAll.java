package com.enonic.ec.kubernetes.operator.commands.apps;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;
import com.enonic.ec.kubernetes.operator.crd.app.client.XpAppCache;
import com.enonic.ec.kubernetes.operator.crd.app.diff.DiffResource;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigCache;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClient;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

@Value.Immutable
public abstract class CommandXpAppApplyAll
    extends Configuration
    implements CombinedCommandBuilder
{
    public abstract XpConfigClient xpConfigClient();

    public abstract XpConfigCache xpConfigCache();

    protected abstract XpAppCache xpAppCache();

    protected abstract ResourceInfoNamespaced<XpAppResource, DiffResource> info();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Get all apps in namespace
        List<XpAppResource> allApps = xpAppCache().getByNamespace( info().deploymentInfo().namespaceName() ).collect( Collectors.toList() );

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
