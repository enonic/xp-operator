package com.enonic.cloud.operator.operators.common.queues;

import java.time.Instant;

import org.immutables.value.Value;

@Value.Immutable
public abstract class QueuedAggregator
{
    public abstract String actionId();

    public abstract ResourceChangeAggregator item();

    @Value.Derived
    public Instant instant()
    {
        return Instant.now();
    }
}
