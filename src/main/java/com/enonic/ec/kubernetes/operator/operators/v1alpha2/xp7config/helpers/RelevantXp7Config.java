package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7config.helpers;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.operators.common.cache.Caches;

@Value.Immutable
public abstract class RelevantXp7Config
    extends Configuration
{
    protected abstract Caches caches();

    protected abstract ConfigMap configMap();

    @Value.Derived
    public List<V1alpha2Xp7Config> xp7Configs()
    {
        // Filter by ConfigMap predicate
        Predicate<V1alpha2Xp7Config> filter = c -> {
            if ( c.getSpec().nodeGroup().equals( cfgStr( "operator.deployment.xp.allNodesKey" ) ) )
            {
                // Apply to all nodes (config maps)
                return true;
            }
            // Filter by node (config map) name
            return c.getSpec().nodeGroup().equals( configMap().getMetadata().getName() );
        };

        return caches().getConfigCache().getByNamespace( configMap().getMetadata().getNamespace() ).
            filter( filter ).
            collect( Collectors.toList() );
    }
}
