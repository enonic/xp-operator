package com.enonic.ec.kubernetes.operator.kubectl;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.Command;

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
