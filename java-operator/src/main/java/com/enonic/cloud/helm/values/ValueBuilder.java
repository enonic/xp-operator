package com.enonic.cloud.helm.values;

import java.util.function.Function;

public interface ValueBuilder<T>
    extends Function<T, Values>
{
}
