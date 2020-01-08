package com.enonic.ec.kubernetes.operator.helm;

import com.enonic.ec.kubernetes.operator.common.commands.Command;

public abstract class HelmCommand
    implements Command<Void>
{
    public abstract Helm helm();

    public abstract String namespace();

    public abstract String name();
}
