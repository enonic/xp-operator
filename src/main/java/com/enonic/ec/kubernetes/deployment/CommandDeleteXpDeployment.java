package com.enonic.ec.kubernetes.deployment;

import com.enonic.ec.kubernetes.common.commands.Command;
import com.enonic.ec.kubernetes.deployment.XpDeployment.XpDeploymentResource;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandDeleteXpDeployment
    implements Command<Boolean>
{
    private final CrdClientsProducer.XpDeploymentClient client;

    private final XpDeploymentResource resource;

    private CommandDeleteXpDeployment( final Builder builder )
    {
        client = assertNotNull( "client", builder.client );
        resource = assertNotNull( "resource", builder.resource );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    public Boolean execute()
    {
        return client.getClient().withName( resource.getMetadata().getName() ).cascading( true ).delete();
    }

    public static final class Builder
    {
        private CrdClientsProducer.XpDeploymentClient client;

        private XpDeploymentResource resource;

        private Builder()
        {
        }

        public Builder client( final CrdClientsProducer.XpDeploymentClient val )
        {
            client = val;
            return this;
        }

        public Builder resource( final XpDeploymentResource val )
        {
            resource = val;
            return this;
        }

        public CommandDeleteXpDeployment build()
        {
            return new CommandDeleteXpDeployment( this );
        }
    }
}
