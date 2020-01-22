package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.commands;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.operators.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.info.DiffXp7App;
import com.enonic.ec.kubernetes.operator.operators.ResourceInfoNamespaced;;

@Value.Immutable
public abstract class CommandXpAppApplyAll
    extends Configuration
    implements CombinedCommandBuilder
{
    public abstract Clients clients();

    public abstract Caches caches();

    protected abstract ResourceInfoNamespaced<V1alpha1Xp7App, DiffXp7App> info();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Get all apps in namespace
        List<V1alpha1Xp7App> allApps =
            caches().getAppCache().getByNamespace( info().deploymentInfo().namespaceName() ).collect( Collectors.toList() );

        // Apply them to the config file
        ImmutableCommandXpAppApply.builder().
            clients( clients() ).
            caches( caches() ).
            info( info() ).
            nodeGroup( cfgStr( "operator.deployment.xp.allNodes" ) ).
            name( cfgStrFmt( "operator.config.xp.deploy.name", cfgStr( "operator.deployment.xp.allNodes" ) ) ).
            file( cfgStr( "operator.config.xp.deploy.file" ) ).
            xpAppResources( allApps ).
            build().
            addCommands( commandBuilder );
    }
}
