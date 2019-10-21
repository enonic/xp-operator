package com.enonic.ec.kubernetes.common.commands;

public abstract class KubeCommand<T>
    implements Command<T>
{
    public abstract KubeCommandSummary summary();
}
