package com.enonic.ec.kubernetes.operator.commands.apply;

import io.fabric8.kubernetes.api.model.ObjectMeta;

import com.enonic.ec.kubernetes.operator.commands.common.CommandApplyResource;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerClientProducer;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerResource;
import com.enonic.ec.kubernetes.operator.crd.certmanager.issuer.IssuerResourceSpec;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandApplyIssuer
    extends CommandApplyResource<IssuerResource>
{
    private final IssuerClientProducer.IssuerClient client;

    private final IssuerResourceSpec spec;

    private CommandApplyIssuer( final Builder builder )
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
    protected IssuerResource fetchResource()
    {
        return client.getClient().inNamespace( namespace ).withName( name ).get();
    }

    @Override
    protected IssuerResource apply( final ObjectMeta metadata )
    {
        IssuerResource issuerResource = new IssuerResource();
        issuerResource.setMetadata( metadata );
        issuerResource.setSpec( spec );
        return client.getClient().inNamespace( namespace ).createOrReplace( issuerResource );
    }

    public static final class Builder
        extends CommandApplyResource.Builder<Builder>
    {
        private IssuerClientProducer.IssuerClient client;

        private IssuerResourceSpec spec;

        public Builder client( final IssuerClientProducer.IssuerClient val )
        {
            client = val;
            return this;
        }

        public Builder spec( final IssuerResourceSpec val )
        {
            spec = val;
            return this;
        }

        public CommandApplyIssuer build()
        {
            return new CommandApplyIssuer( this );
        }
    }
}
