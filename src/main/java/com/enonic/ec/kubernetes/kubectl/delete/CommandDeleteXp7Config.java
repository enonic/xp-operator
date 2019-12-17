package com.enonic.ec.kubernetes.kubectl.delete;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7config.client.Xp7ConfigClient;

@Value.Immutable
public abstract class CommandDeleteXp7Config
    extends CommandDeleteResource
{
    protected abstract Xp7ConfigClient client();

    @Override
    protected String resourceKind()
    {
        return cfgStr( "operator.crd.xp.configs.kind" );
    }

    @Override
    protected Boolean delete()
    {
        return client().client().inNamespace( namespace() ).withName( name() ).delete();
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
