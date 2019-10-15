package com.enonic.ec.kubernetes.operator.commands.apply;

import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.ec.kubernetes.operator.commands.common.CommandApplyResource;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;
import static com.enonic.ec.kubernetes.common.assertions.Assertions.ifNullDefault;

public class CommandApplyConfigMap
    extends CommandApplyResource<ConfigMap>
{
    private final KubernetesClient client;

    private final Map<String, String> data;

    private CommandApplyConfigMap( final Builder builder )
    {
        super( builder );
        client = assertNotNull( "client", builder.client );
        data = ifNullDefault( builder.data, Map.of() );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    protected ConfigMap fetchResource()
    {
        return client.configMaps().inNamespace( namespace ).withName( name ).get();
    }

    @Override
    protected ConfigMap apply( final ObjectMeta metadata )
    {
        ConfigMap configMap = new ConfigMap();
        configMap.setMetadata( metadata );
        configMap.setData( data );
        return client.configMaps().createOrReplace( configMap );
    }

    public static final class Builder
        extends CommandApplyResource.Builder<Builder>
    {
        private KubernetesClient client;

        private Map<String, String> data;

        private Builder()
        {
        }

        public Builder client( final KubernetesClient val )
        {
            client = val;
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
