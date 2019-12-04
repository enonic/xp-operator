package com.enonic.ec.kubernetes.kubectl.delete;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandDeleteStatefulSet
    extends CommandDeleteResource
{
    protected abstract KubernetesClient client();

    @Override
    protected String resourceKind()
    {
        return StatefulSet.class.getSimpleName();
    }

    @Override
    protected Boolean delete()
    {
        return client().apps().statefulSets().inNamespace( namespace() ).withName( name() ).delete();
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
