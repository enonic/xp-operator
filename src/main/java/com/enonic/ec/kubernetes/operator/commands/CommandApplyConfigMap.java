package com.enonic.ec.kubernetes.operator.commands;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.common.commands.Command;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;
import static com.enonic.ec.kubernetes.common.assertions.Assertions.ifNullDefault;

public class CommandApplyConfigMap
    implements Command<ConfigMap>
{
    private final Logger log = LoggerFactory.getLogger( CommandApplyConfigMap.class );

    private final KubernetesClient client;

    private final OwnerReference ownerReference;

    private final String name;

    private final String namespace;

    private final Map<String, String> labels;

    private final Map<String, String> data;

    private CommandApplyConfigMap( final Builder builder )
    {
        client = assertNotNull( "client", builder.client );
        ownerReference = assertNotNull( "ownerReference", builder.ownerReference );
        name = assertNotNull( "name", builder.name );
        namespace = assertNotNull( "namespace", builder.namespace );
        labels = assertNotNull( "labels", builder.labels );
        data = ifNullDefault( builder.data, Map.of() );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    public ConfigMap execute()
    {
        log.debug( "Creating in Namespace '" + namespace + "' ConfigMap '" + name + "'" );
        ObjectMeta metaData = new ObjectMeta();
        metaData.setOwnerReferences( List.of( ownerReference ) );
        metaData.setName( name );
        metaData.setNamespace( namespace );
        metaData.setLabels( labels );

        ConfigMap configMap = new ConfigMap();
        configMap.setMetadata( metaData );
        configMap.setData( data );

        return client.configMaps().inNamespace( namespace ).createOrReplace( configMap );
    }

    public static final class Builder
    {
        private KubernetesClient client;

        private OwnerReference ownerReference;

        private String name;

        private String namespace;

        private Map<String, String> labels;

        private Map<String, String> data;

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

        public Builder namespace( final String val )
        {
            namespace = val;
            return this;
        }

        public Builder labels( final Map<String, String> val )
        {
            labels = val;
            return this;
        }

        public Builder data( final Map<String, String> val )
        {
            data = val;
            return this;
        }

        public CommandApplyConfigMap build()
        {
            return new CommandApplyConfigMap( this );
        }
    }
}
