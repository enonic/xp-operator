package com.enonic.ec.kubernetes.common.commands;

import com.enonic.ec.kubernetes.common.Configuration;

public abstract class KubeCommand<T>
    extends Configuration
    implements Command<T>
{
    public abstract KubeCommandSummary summary();
}
