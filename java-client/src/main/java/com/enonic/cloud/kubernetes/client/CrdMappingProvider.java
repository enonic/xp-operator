package com.enonic.cloud.kubernetes.client;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.fabric8.kubernetes.api.KubernetesResourceMappingProvider;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class CrdMappingProvider
    implements KubernetesResourceMappingProvider
{
    private static final String group = "enonic.cloud";

    private final Map<String, Class<? extends KubernetesResource>> mappings;

    @SuppressWarnings("WeakerAccess")
    public CrdMappingProvider()
    {
        mappings = createMappings();
    }

    @SuppressWarnings("unused")
    public static void register()
    {
        CrdMappingProvider m = new CrdMappingProvider();
        for ( Map.Entry<String, Class<? extends KubernetesResource>> s : m.getMappings().entrySet() )
        {
            KubernetesDeserializer.registerCustomKind( s.getKey(), s.getValue() );
        }
    }

    @SuppressWarnings("unused")
    public static void registerDeserializer( ObjectMapper objectMapper )
    {
        SimpleModule module = new SimpleModule();
        module.addDeserializer( KubernetesResource.class, new KubernetesDeserializer() );
        objectMapper.registerModule( module );
    }

    private Map<String, Class<? extends KubernetesResource>> createMappings()
    {
        Map<String, Class<? extends KubernetesResource>> map = new HashMap<>();

        put( map, "v1alpha1", "Xp7App", com.enonic.cloud.kubernetes.client.v1alpha1.Xp7App.class );
        put( map, "v1alpha2", "Xp7Config", com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Config.class );
        put( map, "v1alpha2", "Xp7Deployment", com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Deployment.class );
        put( map, "v1alpha2", "Domain", com.enonic.cloud.kubernetes.client.v1alpha2.Domain.class );

        return map;
    }

    private void put( Map<String, Class<? extends KubernetesResource>> map, String apiVersion, String kind,
                      Class<? extends KubernetesResource> klass )
    {
        map.put( String.format( "%s/%s#%s", group, apiVersion, kind ), klass );
    }

    @Override
    public Map<String, Class<? extends KubernetesResource>> getMappings()
    {
        return mappings;
    }
}
