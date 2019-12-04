package com.enonic.ec.kubernetes.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyStatefulSet
    extends CommandApplyResource<StatefulSet>
{
    protected abstract KubernetesClient client();

    protected abstract StatefulSetSpec spec();

    @Override
    protected Optional<StatefulSet> fetchResource()
    {
        return Optional.ofNullable( client().apps().statefulSets().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected StatefulSet build( final ObjectMeta metadata )
    {
        StatefulSet statefulSet = new StatefulSet();
        statefulSet.setMetadata( metadata );
        statefulSet.setSpec( spec() );
        return statefulSet;
    }

    @Override
    protected StatefulSet apply( final StatefulSet resource )
    {
        return client().apps().statefulSets().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
