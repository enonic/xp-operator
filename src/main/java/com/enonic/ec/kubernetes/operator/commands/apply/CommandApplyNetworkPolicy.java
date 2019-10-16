package com.enonic.ec.kubernetes.operator.commands.apply;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicySpec;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandApplyNetworkPolicy
    extends CommandApplyResource<NetworkPolicy>
{
    protected abstract KubernetesClient client();

    protected abstract NetworkPolicySpec spec();

    @Override
    protected NetworkPolicy fetchResource()
    {
        return client().network().networkPolicies().inNamespace( namespace() ).withName( name() ).get();
    }

    @Override
    protected NetworkPolicy apply( final ObjectMeta metadata )
    {
        NetworkPolicy networkPolicy = new NetworkPolicy();
        networkPolicy.setMetadata( metadata );
        networkPolicy.setSpec( spec() );
        return client().network().networkPolicies().inNamespace( namespace() ).createOrReplace( networkPolicy );
    }
}
