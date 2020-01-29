package com.enonic.ec.kubernetes.operator.kubectl.newapply.base;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.ec.kubernetes.operator.common.commands.Command;


@Value.Immutable
public abstract class KubeCommand
    implements Command<Void>
{
    protected abstract HasMetadata resource();

    protected abstract KubeCommandAction action();

    protected abstract Command cmd();

    @Override
    public Void execute()
        throws Exception
    {
        cmd().execute();
        return null;
    }

    @Override
    public String toString()
    {
        if ( resource().getMetadata().getNamespace() != null )
        {
            return String.format( "%s in NS '%s' %s '%s'", action(), resource().getMetadata().getNamespace(), resource().getKind(),
                                  resource().getMetadata().getName() );
        }
        return String.format( "%s %s '%s'", action(), resource().getKind(),
                              resource().getMetadata().getName() );
    }
}
