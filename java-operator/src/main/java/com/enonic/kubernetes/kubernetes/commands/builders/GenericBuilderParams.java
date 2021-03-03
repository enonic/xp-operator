package com.enonic.kubernetes.kubernetes.commands.builders;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.common.annotations.Params;

@Value.Immutable
@Params
public interface GenericBuilderParams<R extends HasMetadata>
{
    R resource();

    GenericBuilderAction action();
}
