package com.enonic.ec.kubernetes.operator.commands.apply;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandApplyStatefulSet
    extends CommandApplyResource<StatefulSet>
{
    protected abstract KubernetesClient client();

    protected abstract StatefulSetSpec spec();

    @Override
    protected StatefulSet fetchResource()
    {
        return client().apps().statefulSets().inNamespace( namespace() ).withName( name() ).get();
    }

    @Override
    protected StatefulSet apply( final ObjectMeta metadata )
    {
        StatefulSet statefulSet = new StatefulSet();
        statefulSet.setMetadata( metadata );
        statefulSet.setSpec( spec() );
        return client().apps().statefulSets().inNamespace( namespace() ).createOrReplace( statefulSet );
    }

}
