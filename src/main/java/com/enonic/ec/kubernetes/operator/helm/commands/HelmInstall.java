package com.enonic.ec.kubernetes.operator.helm.commands;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.helm.Chart;
import com.enonic.ec.kubernetes.operator.helm.HelmCommand;

@Value.Immutable
public abstract class HelmInstall
    extends HelmCommand
{
    protected abstract Chart chart();

    protected abstract Object values();

    @Override
    public Void execute()
        throws Exception
    {
        helm().install( chart(), values(), namespace(), name() );
        return null;
    }

    @Override
    public String toString()
    {
        return "HelmInstall{}";
    }
}
