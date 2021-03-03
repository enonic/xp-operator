package com.enonic.kubernetes.helm.values;

import java.util.function.Function;

public interface ValueBuilder<T>
    extends Function<T, Values>
{
}
