package com.enonic.cloud.operator.kubectl.base;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.operator.common.commands.Command;


@Value.Immutable
public abstract class KubeCommand
    implements Command
{
    protected abstract HasMetadata resource();

    protected abstract KubeCommandAction action();

    protected abstract Command cmd();

    @Override
    public void execute()
        throws Exception
    {
        cmd().execute();
    }

    @Override
    public String toString()
    {
        if ( resource().getMetadata().getNamespace() != null )
        {
            return String.format( "%s in NS '%s' %s '%s'", action(), resource().getMetadata().getNamespace(), resource().getKind(),
                                  resource().getMetadata().getName() );
        }
        return String.format( "%s %s '%s'", action(), resource().getKind(), resource().getMetadata().getName() );
    }
}
