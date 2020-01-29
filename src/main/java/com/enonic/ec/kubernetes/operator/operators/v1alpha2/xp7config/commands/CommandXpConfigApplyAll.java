package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.commands;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.operators.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.cache.Caches;
import com.enonic.ec.kubernetes.operator.operators.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.info.DiffXp7Config;


@Value.Immutable
public abstract class CommandXpConfigApplyAll
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract Clients clients();

    protected abstract Caches caches();

    protected abstract ResourceInfoNamespaced<V1alpha2Xp7Config, DiffXp7Config> info();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Iterate over config maps relevant to this XpConfig change
        for ( ConfigMap configMap : getRelevantConfigMaps( info().resource() ) )
        {
            // Get all relevant XpConfig for this ConfigMap
            List<V1alpha2Xp7Config> allXpConfigs = getRelevantXpConfig( configMap );

            // Update ConfigMap
            ImmutableCommandXpConfigApply.builder().
                clients( clients() ).
                configMap( configMap ).
                xpConfigResources( allXpConfigs ).
                build().
                addCommands( commandBuilder );
        }
    }

    private List<ConfigMap> getRelevantConfigMaps( final V1alpha2Xp7Config configResource )
    {
        // Filter by node
        Predicate<ConfigMap> filter = c -> {
            if ( configResource.getSpec().nodeGroup().equals( cfgStr( "operator.deployment.xp.allNodes" ) ) )
            {
                // Apply to all nodes
                return true;
            }
            // Filter by node name
            return configResource.getSpec().nodeGroup().equals( c.getMetadata().getName() );
        };

        return caches().getConfigMapCache().getByNamespace( info().deploymentInfo().namespaceName() ).
            filter( filter ).
            collect( Collectors.toList() );
    }

    private List<V1alpha2Xp7Config> getRelevantXpConfig( final ConfigMap configMap )
    {
        Predicate<V1alpha2Xp7Config> filter = c -> {
            if ( c.getSpec().nodeGroup().equals( cfgStr( "operator.deployment.xp.allNodes" ) ) )
            {
                // Apply to all nodes
                return true;
            }
            // Filter by node name
            return c.getSpec().nodeGroup().equals( configMap.getMetadata().getName() );
        };

        return caches().getConfigCache().getByNamespace( info().deploymentInfo().namespaceName() ).
            filter( filter ).
            collect( Collectors.toList() );
    }
}
