package com.enonic.ec.kubernetes.kubectl.apply;

import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Value.Immutable
public abstract class CommandApplyConfigMap
    extends CommandApplyResource<ConfigMap>
{
    protected abstract KubernetesClient client();

    protected abstract Map<String, String> data();

    @Override
    protected Optional<ConfigMap> fetchResource()
    {
        return Optional.ofNullable( client().configMaps().inNamespace( namespace().get() ).withName( name() ).get() );
    }

    @Override
    protected ConfigMap build( final ObjectMeta metadata )
    {
        ConfigMap configMap = new ConfigMap();
        configMap.setMetadata( metadata );
        configMap.setData( data() );
        return configMap;
    }

    @Override
    protected ConfigMap apply( final ConfigMap resource )
    {
        return client().configMaps().inNamespace( namespace().get() ).createOrReplace( resource );
    }

    @Override
    public String toString()
    {
        return super.toString();
    }
}