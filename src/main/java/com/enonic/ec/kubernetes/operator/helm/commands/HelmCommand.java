package com.enonic.ec.kubernetes.operator.helm.commands;

import com.enonic.ec.kubernetes.operator.common.commands.Command;
import com.enonic.ec.kubernetes.operator.helm.Helm;

public abstract class HelmCommand
    implements Command<Void>
{
    public abstract Helm helm();

    public abstract String namespace();

    public abstract String name();
}
