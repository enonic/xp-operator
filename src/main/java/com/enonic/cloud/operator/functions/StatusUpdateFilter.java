package com.enonic.cloud.operator.functions;

import java.util.function.Predicate;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.common.annotations.Params;

import static com.enonic.cloud.common.Configuration.cfgBool;
import static com.enonic.cloud.common.Configuration.cfgLong;

@Value.Immutable
@Params
public abstract class StatusUpdateFilter
    implements Predicate<HasMetadata>
{
    @Value.Default
    protected Boolean enabled()
    {
        return cfgBool( "operator.status.enabled" );
    }

    @Value.Default
    protected HasMetadataOlderThan hasMetadataOlderThan()
    {
        return HasMetadataOlderThanImpl.of( cfgLong( "operator.tasks.statusDelaySeconds" ) );
    }

    @Override
    public boolean test( final HasMetadata hasMetadata )
    {
        return enabled() && hasMetadata.getMetadata().getDeletionTimestamp() == null && hasMetadataOlderThan().test( hasMetadata );
    }
}
