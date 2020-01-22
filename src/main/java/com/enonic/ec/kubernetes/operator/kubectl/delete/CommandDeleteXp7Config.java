package com.enonic.ec.kubernetes.operator.kubectl.delete;

import org.immutables.value.Value;

@Value.Immutable
public abstract class CommandDeleteXp7Config
    extends CommandDeleteResource
{
    @Override
    protected String resourceKind()
    {
        return cfgStr( "operator.crd.configs.kind" );
    }

    @Override
    protected Boolean delete()
    {
        return clients().getConfigClient().inNamespace( namespace() ).withName( name() ).delete();
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
