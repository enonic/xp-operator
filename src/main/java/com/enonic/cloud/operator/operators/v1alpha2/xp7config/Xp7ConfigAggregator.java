package com.enonic.cloud.operator.operators.v1alpha2.xp7config;


import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.ImmutableV1alpha2Xp7ConfigSpec;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.operator.operators.common.queues.NodeGroupChangeAggregator;

public abstract class Xp7ConfigAggregator
    extends NodeGroupChangeAggregator<V1alpha2Xp7Config>
{
    public abstract ObjectMeta metadata();

    public abstract String nodeGroup();

    protected abstract String file();

    protected abstract String data();

    @Override
    protected V1alpha2Xp7Config buildModification()
    {
        V1alpha2Xp7Config config = new V1alpha2Xp7Config();
        config.setMetadata( metadata() );
        config.setSpec( ImmutableV1alpha2Xp7ConfigSpec.builder().
            nodeGroup( nodeGroup() ).
            file( file() ).
            data( data() ).
            build() );

        return config;
    }
}
