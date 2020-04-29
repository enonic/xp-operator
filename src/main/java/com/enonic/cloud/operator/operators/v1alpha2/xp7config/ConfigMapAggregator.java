package com.enonic.cloud.operator.operators.v1alpha2.xp7config;

import java.util.HashMap;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.common.queues.NodeGroupChangeAggregator;

@Value.Immutable
public abstract class ConfigMapAggregator
    extends NodeGroupChangeAggregator<ConfigMap>
{
    protected abstract Caches caches();

    protected abstract ObjectMeta metadata();

    @Override
    protected ConfigMap buildModification()
    {
        ConfigMap cm = new ConfigMap();
        cm.setMetadata( metadata() );

        Map<String, String> data = new HashMap<>();

        caches().getConfigCache().
            getByNamespace( metadata().getNamespace() ).
            filter( c -> matchNodeGroup( metadata().getName(), c.getSpec().nodeGroup() ) ).
            forEach( c -> data.put( c.getSpec().file(), c.getSpec().data() ) );

        cm.setData( data );

        return cm;
    }
}

