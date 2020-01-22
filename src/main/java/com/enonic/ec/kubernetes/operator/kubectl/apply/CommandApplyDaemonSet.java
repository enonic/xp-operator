package com.enonic.ec.kubernetes.operator.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.operators.clients.Clients;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyDaemonSet
    extends CommandApplyResource<DaemonSet>
{
    protected abstract DaemonSetSpec spec();

    @Override
    protected Optional<DaemonSet> fetchResource()
    {
        return Optional.ofNullable( clients().getDefaultClient().apps().daemonSets().inNamespace( namespace().get() ).withName( name() ).get() );
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
        return clients().getDefaultClient().apps().daemonSets().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
