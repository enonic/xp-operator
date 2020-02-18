package com.enonic.cloud.operator.operators.v1alpha2.xp7config.commands;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;

import com.enonic.cloud.operator.common.commands.CombinedCommandBuilder;
import com.enonic.cloud.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.operator.operators.common.ResourceInfoNamespaced;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.common.clients.Clients;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.helpers.ImmutableRelevantXp7Config;
import com.enonic.cloud.operator.operators.v1alpha2.xp7config.info.DiffXp7Config;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;


@Value.Immutable
public abstract class CommandConfigMapUpdateAll
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
            List<V1alpha2Xp7Config> allXpConfigs = ImmutableRelevantXp7Config.builder().
                caches( caches() ).
                configMap( configMap ).
                build().
                xp7Configs();

            // Update ConfigMap
            ImmutableCommandConfigMapUpdate.builder().
                clients( clients() ).
                configMap( configMap ).
                xpConfigResources( allXpConfigs ).
                build().
                addCommands( commandBuilder );
        }
    }

    private List<ConfigMap> getRelevantConfigMaps( final V1alpha2Xp7Config configResource )
    {
        // Filter by node predicate
        Predicate<ConfigMap> filter = c -> {
            if ( c.getMetadata().getLabels().get( "nodeGroup" ) == null )
            {
                // This is not a node config, ignore it
                return false;
            }
            if ( configResource.getSpec().nodeGroup().equals( cfgStr( "operator.deployment.xp.allNodesKey" ) ) )
            {
                // Apply to all nodes (config maps)
                return true;
            }
            // Filter by node (config map) name
            return configResource.getSpec().nodeGroup().equals( c.getMetadata().getName() );
        };

        return caches().getConfigMapCache().getByNamespace( info().deploymentInfo().namespaceName() ).
            filter( filter ).
            collect( Collectors.toList() );
    }
}
