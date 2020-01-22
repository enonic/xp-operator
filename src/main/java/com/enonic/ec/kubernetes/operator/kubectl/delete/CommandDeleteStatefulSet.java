package com.enonic.ec.kubernetes.operator.kubectl.delete;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandDeleteStatefulSet
    extends CommandDeleteResource
{
    @Override
    protected String resourceKind()
    {
        return StatefulSet.class.getSimpleName();
    }

    @Override
    protected Boolean delete()
    {
        return clients().getDefaultClient().apps().statefulSets().inNamespace( namespace() ).withName( name() ).delete();
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
