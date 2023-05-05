package com.enonic.kubernetes.client;

import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.client.v1.xp7app.Xp7App;
import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

import java.util.HashMap;
import java.util.Map;

public class CrdMappingProvider
{
    private static final String API_VERSION = "enonic.cloud/v1";

    @SuppressWarnings("unused")
    public static void register()
    {
        KubernetesDeserializer.registerCustomKind( API_VERSION, "Xp7App", Xp7App.class );
        KubernetesDeserializer.registerCustomKind( API_VERSION, "Xp7Config", Xp7Config.class );
        KubernetesDeserializer.registerCustomKind( API_VERSION, "Xp7Deployment", Xp7Deployment.class );
        KubernetesDeserializer.registerCustomKind( API_VERSION, "Domain", Domain.class );
    }

    @SuppressWarnings("unused")
    public static void registerDeserializer( ObjectMapper objectMapper )
    {
        SimpleModule module = new SimpleModule();
        module.addDeserializer( KubernetesResource.class, new KubernetesDeserializer() );
        objectMapper.registerModule( module );
    }

    /*private Map<String, Class<? extends KubernetesResource>> createMappings()
    {
        Map<String, Class<? extends KubernetesResource>> map = new HashMap<>();

        put( map, "v1", "Xp7App", Xp7App.class );
        put( map, "v1", "Xp7Config", Xp7Config.class );
        put( map, "v1", "Xp7Deployment", Xp7Deployment.class );
        put( map, "v1", "Domain", Domain.class );

        return map;
    }*/

 /*   private void put( Map<String, Class<? extends KubernetesResource>> map, String apiVersion, String kind,
                      Class<? extends KubernetesResource> klass )
    {
        map.put( String.format( "%s/%s#%s", group, apiVersion, kind ), klass );
    }*/
}
