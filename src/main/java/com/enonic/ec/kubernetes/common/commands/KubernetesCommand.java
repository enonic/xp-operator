package com.enonic.ec.kubernetes.common.commands;

import com.enonic.ec.kubernetes.common.Configuration;

public abstract class KubernetesCommand<T>
    extends Configuration
    implements Command<T>
{
    public abstract KubernetesCommandSummary summary();

    @Override
    public String toString()
    {
        return summary().toString();
    }
}
