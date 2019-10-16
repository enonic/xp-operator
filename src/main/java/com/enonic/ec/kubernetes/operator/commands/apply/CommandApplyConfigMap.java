package com.enonic.ec.kubernetes.operator.commands.apply;

import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;

@Value.Immutable
public abstract class CommandApplyConfigMap
    extends CommandApplyResource<ConfigMap>
{
    protected abstract KubernetesClient client();

    protected abstract Map<String, String> data();

    @Override
    protected ConfigMap fetchResource()
    {
        return client().configMaps().inNamespace( namespace() ).withName( name() ).get();
    }

    @Override
    protected ConfigMap apply( final ObjectMeta metadata )
    {
        ConfigMap configMap = new ConfigMap();
        configMap.setMetadata( metadata );
        configMap.setData( data() );
        return client().configMaps().inNamespace( namespace() ).createOrReplace( configMap );
    }

}