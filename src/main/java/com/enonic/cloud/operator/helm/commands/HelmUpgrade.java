package com.enonic.cloud.operator.helm.commands;

import org.immutables.value.Value;

import com.enonic.cloud.operator.helm.Chart;

@Value.Immutable
public abstract class HelmUpgrade
    extends HelmCommand
{
    protected abstract Chart chart();

    protected abstract Object values();

    @Override
    public void execute()
        throws Exception
    {
        helm().upgrade( chart(), values(), namespace(), name() );
    }

    @Override
    public String toString()
    {
        return String.format( "%s in NS '%s' helm chart %s", "UPGRADE", namespace(), name() );
    }
}
