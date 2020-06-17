package com.enonic.cloud.operator.functions;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public abstract class HasMetadataOlderThan
    implements Predicate<HasMetadata>
{

    public abstract Long seconds();

    @Override
    public boolean test( final HasMetadata hasMetadata )
    {
        return Duration.between( Instant.parse( hasMetadata.getMetadata().getCreationTimestamp() ), Instant.now() ).getSeconds() >
            seconds();
    }
}
