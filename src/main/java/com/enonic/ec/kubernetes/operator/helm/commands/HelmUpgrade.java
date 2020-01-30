package com.enonic.ec.kubernetes.operator.helm.commands;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.helm.Chart;

@Value.Immutable
public abstract class HelmUpgrade
    extends HelmCommand
{
    protected abstract Chart chart();

    protected abstract Object values();

    @Override
    public Void execute()
        throws Exception
    {
        helm().upgrade( chart(), values(), namespace(), name() );
        return null;
    }

    @Override
    public String toString()
    {
        return "HelmUpgrade{}";
    }
}
