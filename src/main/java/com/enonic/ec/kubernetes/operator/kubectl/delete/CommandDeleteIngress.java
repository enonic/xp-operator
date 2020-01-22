package com.enonic.ec.kubernetes.operator.kubectl.delete;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandDeleteIngress
    extends CommandDeleteResource
{
    @Override
    protected String resourceKind()
    {
        return Ingress.class.getSimpleName();
    }

    @Override
    protected Boolean delete()
    {
        return clients().getDefaultClient().extensions().ingresses().inNamespace( namespace() ).withName( name() ).delete();
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
