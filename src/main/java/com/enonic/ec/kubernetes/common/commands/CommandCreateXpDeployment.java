package com.enonic.ec.kubernetes.common.commands;

import com.enonic.ec.kubernetes.common.crd.CrdClientsProducer;
import com.enonic.ec.kubernetes.common.crd.XpDeployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.common.crd.XpDeployment.XpDeploymentResourceSpec;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertEquals;
import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandCreateXpDeployment
    implements Command<XpDeploymentResource>
{
    private final CrdClientsProducer.XpDeploymentClient client;

    private final String apiVersion;

    private final XpDeploymentResourceSpec spec;

    private CommandCreateXpDeployment( final Builder builder )
    {
        client = assertNotNull( "client", builder.client );
        spec = assertNotNull( "spec", builder.spec );
        apiVersion = assertNotNull( "apiVersion", builder.apiVersion );
        assertNotNull( "spec.cloud", builder.spec.getCloud() );
        assertNotNull( "spec.name", builder.spec.getName() );
        assertNotNull( "spec.project", builder.spec.getProject() );
        assertEquals( "Only version 'enonic.cloud/v1alpha1' allowed", builder.apiVersion, "enonic.cloud/v1alpha1" );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    public XpDeploymentResource execute()
    {
        XpDeploymentResource newDeployment = new XpDeploymentResource();
        newDeployment.setApiVersion( "enonic.cloud/v1alpha1" );
        newDeployment.setKind( "XPDeployment" );
        newDeployment.getMetadata().setName( spec.getFullName() );
        newDeployment.getMetadata().setLabels( spec.getDefaultLabels() );
        newDeployment.setSpec( spec );
        return client.getClient().create( newDeployment );
    }

    public static final class Builder
    {
        private CrdClientsProducer.XpDeploymentClient client;

        private String apiVersion;

        private XpDeploymentResourceSpec spec;

        private Builder()
        {
        }

        public Builder client( final CrdClientsProducer.XpDeploymentClient val )
        {
            client = val;
            return this;
        }

        public Builder apiVersion( final String val )
        {
            apiVersion = val;
            return this;
        }

        public Builder spec( final XpDeploymentResourceSpec val )
        {
            spec = val;
            return this;
        }

        public CommandCreateXpDeployment build()
        {
            return new CommandCreateXpDeployment( this );
        }
    }
}
