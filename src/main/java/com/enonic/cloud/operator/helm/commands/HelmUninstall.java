package com.enonic.cloud.operator.helm.commands;

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
        return String.format( "%s in NS '%s' helm chart %s", "UNINSTALL", namespace(), name() );
    }
}
