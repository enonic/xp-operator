package com.enonic.ec.kubernetes.operator.info;

@FunctionalInterface
public interface TriFunction<S, T, U, R>
{
    R apply( S var1, T var2, U var3 );
}
