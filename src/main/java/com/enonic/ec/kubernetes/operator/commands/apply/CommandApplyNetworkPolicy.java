package com.enonic.ec.kubernetes.operator.commands.apply;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicySpec;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.commands.common.CommandApplyResource;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandApplyNetworkPolicy
    extends CommandApplyResource<NetworkPolicy>
{
    private final KubernetesClient client;

    private final NetworkPolicySpec spec;

    private CommandApplyNetworkPolicy( final Builder builder )
    {
        super( builder );
        client = assertNotNull( "client", builder.client );
        spec = assertNotNull( "spec", builder.spec );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    protected NetworkPolicy fetchResource()
    {
        return client.network().networkPolicies().inNamespace( namespace ).withName( name ).get();
    }

    @Override
    protected NetworkPolicy apply( final ObjectMeta metadata )
    {
        NetworkPolicy networkPolicy = new NetworkPolicy();
        networkPolicy.setMetadata( metadata );
        networkPolicy.setSpec( spec );
        return client.network().networkPolicies().createOrReplace( networkPolicy );
    }

    public static final class Builder
        extends CommandApplyResource.Builder<Builder>
    {
        private KubernetesClient client;

        private NetworkPolicySpec spec;

        public Builder client( final KubernetesClient val )
        {
            client = val;
            return this;
        }

        public Builder spec( final NetworkPolicySpec val )
        {
            spec = val;
            return this;
        }

        public CommandApplyNetworkPolicy build()
        {
            return new CommandApplyNetworkPolicy( this );
        }
    }
}
