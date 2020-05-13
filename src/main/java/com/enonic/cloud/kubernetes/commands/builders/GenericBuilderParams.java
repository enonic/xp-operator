package com.enonic.cloud.kubernetes.commands.builders;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public interface GenericBuilderParams<R extends HasMetadata>
{
    R resource();

    GenericBuilderAction action();

    @Value.Default
    default boolean neverModify()
    {
        return false;
    }

    @Value.Default
    default boolean alwaysOverwrite()
    {
        return false;
    }
}
