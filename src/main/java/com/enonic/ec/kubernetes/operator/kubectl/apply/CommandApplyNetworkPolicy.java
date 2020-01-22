package com.enonic.ec.kubernetes.operator.kubectl.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicySpec;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.operators.clients.Clients;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyNetworkPolicy
    extends CommandApplyResource<NetworkPolicy>
{
    protected abstract NetworkPolicySpec spec();

    @Override
    protected Optional<NetworkPolicy> fetchResource()
    {
        return Optional.ofNullable( clients().getDefaultClient().network().networkPolicies().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected NetworkPolicy build( final ObjectMeta metadata )
    {
        NetworkPolicy networkPolicy = new NetworkPolicy();
        networkPolicy.setMetadata( metadata );
        networkPolicy.setSpec( spec() );
        return networkPolicy;
    }

    @Override
    protected NetworkPolicy apply( final NetworkPolicy resource )
    {
        return clients().getDefaultClient().network().networkPolicies().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}
