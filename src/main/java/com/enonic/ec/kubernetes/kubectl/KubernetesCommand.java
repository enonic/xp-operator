package com.enonic.ec.kubernetes.kubectl;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.Command;

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
