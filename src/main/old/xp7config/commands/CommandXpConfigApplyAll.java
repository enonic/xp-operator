package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.commands;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client.Xp7ConfigCache;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.info.DiffXp7Config;

@Value.Immutable
public abstract class CommandXpConfigApplyAll
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract KubernetesClient defaultClient();

    protected abstract ConfigMapCache configMapCache();

    protected abstract Xp7ConfigCache xpConfigCache();

    protected abstract ResourceInfoNamespaced<Xp7ConfigResource, DiffXp7Config> info();

    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Iterate over config maps relevant to this XpConfig change
        for ( ConfigMap configMap : getRelevantConfigMaps( info().resource() ) )
        {
            // Get all relevant XpConfig for this ConfigMap
            List<Xp7ConfigResource> allXpConfigs = getRelevantXpConfig( configMap );

            // Update ConfigMap
            ImmutableCommandXpConfigApply.builder().
                defaultClient( defaultClient() ).
                configMap( configMap ).
                xpConfigResources( allXpConfigs ).
                build().
                addCommands( commandBuilder );
        }
    }

    private List<ConfigMap> getRelevantConfigMaps( final Xp7ConfigResource configResource )
    {
        // Filter by node
        Predicate<ConfigMap> filter = c -> {
            if ( configResource.getSpec().node().equals( cfgStr( "operator.deployment.xp.allNodesKey" ) ) )
            {
                // Apply to all nodes
                return true;
            }
            // Filter by node name
            return configResource.getSpec().node().equals( c.getMetadata().getName() );
        };

        return configMapCache().getByNamespace( info().deploymentInfo().namespaceName() ).
            filter( filter ).
            collect( Collectors.toList() );
    }

    private List<Xp7ConfigResource> getRelevantXpConfig( final ConfigMap configMap )
    {
        Predicate<Xp7ConfigResource> filter = c -> {
            if ( c.getSpec().node().equals( cfgStr( "operator.deployment.xp.allNodesKey" ) ) )
            {
                // Apply to all nodes
                return true;
            }
            // Filter by node name
            return c.getSpec().node().equals( configMap.getMetadata().getName() );
        };

        return xpConfigCache().getByNamespace( info().deploymentInfo().namespaceName() ).
            filter( filter ).
            collect( Collectors.toList() );
    }
}
