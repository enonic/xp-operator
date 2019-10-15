package com.enonic.ec.kubernetes.operator.commands.apply;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.commands.common.CommandApplyResource;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandApplyService
    extends CommandApplyResource<Service>
{
    private final KubernetesClient client;

    private final ServiceSpec spec;

    private CommandApplyService( final Builder builder )
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
    protected Service fetchResource()
    {
        return client.services().inNamespace( namespace ).withName( name ).get();
    }

    @Override
    protected Service apply( final ObjectMeta metadata )
    {
        Service service = new Service();
        service.setMetadata( metadata );
        service.setSpec( spec );
        return client.services().createOrReplace( service );
    }

    public static final class Builder
        extends CommandApplyResource.Builder<Builder>
    {
        private KubernetesClient client;

        private ServiceSpec spec;

        public Builder client( final KubernetesClient val )
        {
            client = val;
            return this;
        }

        public Builder spec( final ServiceSpec val )
        {
            spec = val;
            return this;
        }

        public CommandApplyService build()
        {
            return new CommandApplyService( this );
        }
    }
}
