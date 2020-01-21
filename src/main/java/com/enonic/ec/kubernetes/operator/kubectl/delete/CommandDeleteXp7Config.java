package com.enonic.ec.kubernetes.operator.kubectl.delete;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client.Xp7ConfigClient;

@Value.Immutable
public abstract class CommandDeleteXp7Config
    extends CommandDeleteResource
{
    protected abstract Xp7ConfigClient client();

    @Override
    protected String resourceKind()
    {
        return cfgStr( "operator.crd.configs.kind" );
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
