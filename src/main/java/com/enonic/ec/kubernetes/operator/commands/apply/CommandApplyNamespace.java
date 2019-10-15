package com.enonic.ec.kubernetes.operator.commands.apply;

import io.fabric8.kubernetes.api.model.HasMetadata;
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
        super( builder.namespace( "null" ) ); // Creating a namespace does not require a namespace
        client = assertNotNull( "client", builder.client );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    protected void checkValidity( final HasMetadata resource )
        throws Exception
    {
        if ( !resource.getMetadata().getName().equals( name ) )
        {
            throw new Exception( "Resource names do not match" );
        }
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
