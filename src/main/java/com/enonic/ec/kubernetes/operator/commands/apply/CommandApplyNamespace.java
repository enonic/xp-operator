package com.enonic.ec.kubernetes.operator.commands.apply;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.commands.common.CommandApplyResource;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandApplyNamespace
    extends CommandApplyResource<Namespace>
{
    private final KubernetesClient client;

    private CommandApplyNamespace( final Builder builder )
    {
        super( builder );
        client = assertNotNull( "client", builder.client );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    protected Namespace fetchResource()
    {
        return client.namespaces().withName( name ).get();
    }

    @Override
    protected Namespace apply( final ObjectMeta metadata )
    {
        Namespace namespace = new Namespace();
        namespace.setMetadata( metadata );
        return client.namespaces().createOrReplace( namespace );
    }

    public static final class Builder
        extends CommandApplyResource.Builder<Builder>
    {
        private KubernetesClient client;

        public Builder client( final KubernetesClient val )
        {
            client = val;
            return this;
        }

        public CommandApplyNamespace build()
        {
            return new CommandApplyNamespace( this );
        }
    }
}
