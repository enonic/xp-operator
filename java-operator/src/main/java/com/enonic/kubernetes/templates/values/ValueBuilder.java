package com.enonic.kubernetes.templates.values;

import java.util.function.Function;

public interface ValueBuilder<T>
    extends Function<T, Values>
{
}
