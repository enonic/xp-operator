package com.enonic.ec.kubernetes.operator.commands.apply;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.commands.common.CommandApplyResource;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandApplyIngress
    extends CommandApplyResource<Ingress>
{
    private final KubernetesClient client;

    private final IngressSpec spec;

    private CommandApplyIngress( final Builder builder )
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
    protected Ingress fetchResource()
    {
        return client.extensions().ingresses().inNamespace( namespace ).withName( name ).get();
    }

    @Override
    protected Ingress apply( final ObjectMeta metadata )
    {
        Ingress ingress = new Ingress();
        ingress.setMetadata( metadata );
        ingress.setSpec( spec );
        return client.extensions().ingresses().createOrReplace( ingress );
    }

    public static final class Builder
        extends CommandApplyResource.Builder<Builder>
    {
        private KubernetesClient client;

        private IngressSpec spec;

        public Builder client( final KubernetesClient val )
        {
            client = val;
            return this;
        }

        public Builder spec( final IngressSpec val )
        {
            spec = val;
            return this;
        }

        public CommandApplyIngress build()
        {
            return new CommandApplyIngress( this );
        }
    }
}
