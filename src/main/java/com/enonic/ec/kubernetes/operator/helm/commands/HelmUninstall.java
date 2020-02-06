package com.enonic.ec.kubernetes.operator.helm.commands;

import org.immutables.value.Value;

@Value.Immutable
public abstract class HelmUninstall
    extends HelmCommand
{
    @Override
    public void execute()
        throws Exception
    {
        helm().uninstall( namespace(), name() );
    }

    @Override
    public String toString()
    {
        return "HelmUninstall{}";
    }
}
