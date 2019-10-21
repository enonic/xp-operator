package com.enonic.ec.kubernetes.operator.commands.delete;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandDeleteService
    extends CommandDeleteResource
{
    protected abstract KubernetesClient client();

    @Override
    protected String resourceKind()
    {
        return Service.class.getSimpleName();
    }

    @Override
    protected Boolean delete()
    {
        return client().services().inNamespace( namespace() ).withName( name() ).delete();
    }
}
