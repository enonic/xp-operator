package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.commands;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.operators.common.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.common.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7app.info.DiffXp7App;

import static com.enonic.ec.kubernetes.operator.common.Configuration.cfgStr;
import static com.enonic.ec.kubernetes.operator.common.Configuration.cfgStrFmt;

@Value.Immutable
public abstract class CommandXpAppsApply
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

        // Apply them to the deploy config file
        ImmutableCommandXpUpdateDeployConfigFile.builder().
            clients( clients() ).
            caches( caches() ).
            info( info() ).
            nodeGroup( cfgStr( "operator.deployment.xp.allNodesKey" ) ).
            name( cfgStrFmt( "operator.deployment.xp.config.deploy.nameTemplate", cfgStr( "operator.deployment.xp.allNodesKey" ) ) ).
            file( cfgStr( "operator.deployment.xp.config.deploy.file" ) ).
            xpAppResources( allApps ).
            build().
            addCommands( commandBuilder );
    }
}
