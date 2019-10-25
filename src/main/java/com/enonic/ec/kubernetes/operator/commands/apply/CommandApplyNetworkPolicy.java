package com.enonic.ec.kubernetes.operator.commands.apply;

import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicySpec;
import io.fabric8.kubernetes.client.KubernetesClient;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyNetworkPolicy
    extends CommandApplyResource<NetworkPolicy>
{
    protected abstract KubernetesClient client();

    protected abstract NetworkPolicySpec spec();

    @Override
    protected Optional<NetworkPolicy> fetchResource()
    {
        return Optional.ofNullable( client().network().networkPolicies().inNamespace( namespace().get() ).withName( name() ).get() );
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
        return client().network().networkPolicies().inNamespace( namespace().get() ).createOrReplace( resource );
    }
}
