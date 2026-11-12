package com.enonic.kubernetes.kubernetes.commands.builders;

import org.jspecify.annotations.NullMarked;


import io.fabric8.kubernetes.api.model.HasMetadata;


@NullMarked
public record GenericBuilderParams<R extends HasMetadata>( R resource, GenericBuilderAction action )
{
}
