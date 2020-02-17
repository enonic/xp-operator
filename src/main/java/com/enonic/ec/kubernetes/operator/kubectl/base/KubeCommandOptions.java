package com.enonic.ec.kubernetes.operator.kubectl.base;

import org.immutables.value.Value;

@Value.Immutable
public class KubeCommandOptions
{
    @Value.Default
    protected boolean replaceOld()
    {
        return false;
    }

    @Value.Default
    protected boolean neverOverwrite()
    {
        return false;
    }
}
