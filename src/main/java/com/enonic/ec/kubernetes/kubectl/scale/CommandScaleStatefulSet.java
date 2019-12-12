package com.enonic.ec.kubernetes.kubectl.scale;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandScaleStatefulSet
    extends CommandScaleResource<StatefulSet>
{
    protected abstract KubernetesClient client();

    @Override
    protected String resourceKind()
    {
        return StatefulSet.class.getSimpleName();
    }

    @Override
    protected StatefulSet applyScale()
    {
        return client().apps().statefulSets().
            inNamespace( namespace() ).
            withName( name() ).
            scale( scale() );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}