package com.enonic.ec.kubernetes.operator.commands.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyDaemonSet
    extends CommandApplyResource<DaemonSet>
{
    protected abstract KubernetesClient client();

    protected abstract DaemonSetSpec spec();

    @Override
    protected Optional<DaemonSet> fetchResource()
    {
        return Optional.ofNullable( client().apps().daemonSets().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected DaemonSet build( final ObjectMeta metadata )
    {
        DaemonSet daemonSet = new DaemonSet();
        daemonSet.setMetadata( metadata );
        daemonSet.setSpec( spec() );
        return daemonSet;
    }

    @Override
    protected DaemonSet apply( final DaemonSet resource )
    {
        return client().apps().daemonSets().inNamespace( namespace().get() ).createOrReplace( resource );
    }
}
