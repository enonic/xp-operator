package com.enonic.ec.kubernetes.operator.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.Command;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandDeleteNamespace
    implements Command<Boolean>
{
    private final Logger log = LoggerFactory.getLogger( CommandDeleteNamespace.class );

    private final KubernetesClient client;

    private final String name;

    private CommandDeleteNamespace( final Builder builder )
    {
        client = assertNotNull( "client", builder.client );
        name = assertNotNull( "name", builder.name );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    public Boolean execute()
    {
        log.debug( "Deleting Namespace '" + name + "'" );
        return client.namespaces().withName( name ).delete();
    }

    public static final class Builder
    {
        private KubernetesClient client;

        private String name;

        private Builder()
        {
        }

        public Builder client( final KubernetesClient val )
        {
            client = val;
            return this;
        }

        public Builder name( final String val )
        {
            name = val;
            return this;
        }

        public CommandDeleteNamespace build()
        {
            return new CommandDeleteNamespace( this );
        }
    }
}
