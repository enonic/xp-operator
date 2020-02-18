package com.enonic.cloud.operator.helm.commands;

import com.enonic.cloud.operator.common.commands.Command;
import com.enonic.cloud.operator.helm.Helm;

public abstract class HelmCommand
    implements Command
{
    public abstract Helm helm();

    public abstract String namespace();

    public abstract String name();
}
