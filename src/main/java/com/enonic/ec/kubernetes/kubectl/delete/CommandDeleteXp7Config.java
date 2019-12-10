package com.enonic.ec.kubernetes.kubectl.delete;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClient;

@Value.Immutable
public abstract class CommandDeleteXp7Config
    extends CommandDeleteResource
{
    protected abstract XpConfigClient client();

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
