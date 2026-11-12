package com.enonic.kubernetes.operator.api.mutation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record Patch( String op, String path, @Nullable Object value )
{
}
