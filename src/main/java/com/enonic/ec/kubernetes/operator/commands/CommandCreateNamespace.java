package com.enonic.ec.kubernetes.operator.commands;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.Command;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandCreateNamespace
    implements Command<Namespace>
{
    private final Logger log = LoggerFactory.getLogger( CommandCreateNamespace.class );

    private final KubernetesClient client;

    private final OwnerReference ownerReference;

    private final String name;

    private CommandCreateNamespace( final Builder builder )
    {
        client = assertNotNull( "client", builder.client );
        ownerReference = assertNotNull( "ownerReference", builder.ownerReference );
        name = assertNotNull( "name", builder.name );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    public Namespace execute()
        throws NamespaceExists
    {
        log.debug( "Creating Namespace '" + name + "'" );
        if ( client.namespaces().withName( name ).get() != null )
        {
            throw new NamespaceExists();
        }
        ObjectMeta metaData = new ObjectMeta();
        metaData.setOwnerReferences( List.of( ownerReference ) );
        metaData.setName( name );

        Namespace ns = new Namespace();
        ns.setMetadata( metaData );

        return client.namespaces().create( ns );
    }

    public static final class NamespaceExists
        extends Exception
    {
    }

    public static final class Builder
    {
        private KubernetesClient client;

        private OwnerReference ownerReference;

        private String name;

        private Builder()
        {
        }

        public Builder client( final KubernetesClient val )
        {
            client = val;
            return this;
        }

        public Builder ownerReference( final OwnerReference val )
        {
            ownerReference = val;
            return this;
        }

        public Builder name( final String val )
        {
            name = val;
            return this;
        }

        public CommandCreateNamespace build()
        {
            return new CommandCreateNamespace( this );
        }
    }
}
