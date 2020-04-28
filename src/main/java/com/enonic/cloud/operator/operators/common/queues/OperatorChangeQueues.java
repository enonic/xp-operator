package com.enonic.cloud.operator.operators.common.queues;

import java.time.Duration;
import java.util.Timer;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMap;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;

@Singleton
public class OperatorChangeQueues
{
    private final ResourceChangeQueue<ConfigMap> configMapResourceChangeQueue;

    private final ResourceChangeQueue<V1alpha2Xp7Config> v1alpha2Xp7ConfigResourceChangeQueue;

    @Inject
    public OperatorChangeQueues()
    {
        Timer timer = new Timer();
        this.configMapResourceChangeQueue = new ResourceChangeQueue<>( Duration.ofMillis( 1500 ) );
        timer.schedule( configMapResourceChangeQueue, 0L, 500L );

        this.v1alpha2Xp7ConfigResourceChangeQueue = new ResourceChangeQueue<>( Duration.ofMillis( 750 ) );
        timer.schedule( v1alpha2Xp7ConfigResourceChangeQueue, 0L, 500L );
    }

    public ResourceChangeQueue<ConfigMap> getConfigMapResourceChangeQueue()
    {
        return configMapResourceChangeQueue;
    }

    public ResourceChangeQueue<V1alpha2Xp7Config> getV1alpha2Xp7ConfigResourceChangeQueue()
    {
        return v1alpha2Xp7ConfigResourceChangeQueue;
    }
}
