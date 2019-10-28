package com.enonic.ec.kubernetes.operator.commands.kubectl.delete;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandDeleteIngress
    extends CommandDeleteResource
{
    protected abstract KubernetesClient client();

    @Override
    protected String resourceKind()
    {
        return Ingress.class.getSimpleName();
    }

    @Override
    protected Boolean delete()
    {
        return client().extensions().ingresses().inNamespace( namespace() ).withName( name() ).delete();
    }
}
