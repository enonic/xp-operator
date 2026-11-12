package com.enonic.kubernetes.client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnableKubernetesMockClient(crud = true)
public class SchemaTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    private KubernetesClient kubernetesClient;
    private KubernetesMockServer server;
    @Test
    void v1Xp7App()
        throws IOException
    {
        CustomResourceDefinition customResourceDefinition = loadCRD( "/apps.yaml" );
        JSONSchemaProps schema = loadSchema( "/schema/v1/xp7app/xp7App.json" );

        assertSchema( getSchemaVersion( customResourceDefinition, "v1" ), schema );
    }

    @Test
    void v1Xp7Config()
        throws IOException
    {
        CustomResourceDefinition customResourceDefinition = loadCRD( "/configs.yaml" );
        JSONSchemaProps schema = loadSchema( "/schema/v1/xp7config/xp7Config.json" );

        assertSchema( getSchemaVersion( customResourceDefinition, "v1" ), schema );
    }

    @Test
    void v1Xp7Deployment()
        throws IOException
    {
        CustomResourceDefinition customResourceDefinition = loadCRD( "/deployments.yaml" );
        JSONSchemaProps schema = loadSchema( "/schema/v1/xp7deployment/xp7Deployment.json" );

        assertSchema( getSchemaVersion( customResourceDefinition, "v1" ), schema );
    }

    private CustomResourceDefinition loadCRD( final String file )
    {
        CustomResourceDefinition customResourceDefinition = kubernetesClient
            .apiextensions()
            .v1()
            .customResourceDefinitions()
            .load( getClass().getResourceAsStream( file ) )
            .item();
        assertNotNull( customResourceDefinition );
        return customResourceDefinition;
    }

    @SuppressWarnings("SameParameterValue")
    private JSONSchemaProps getSchemaVersion( final CustomResourceDefinition crd, final String version )
    {
        for (CustomResourceDefinitionVersion v : crd.getSpec().getVersions()) {
            if (v.getName().equals( version )) {
                return v.getSchema().getOpenAPIV3Schema();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private JSONSchemaProps loadSchema( final String file )
        throws IOException
    {
        Map<String, Object> map = mapper.readValue( getClass().getResource( file ), Map.class );
        cleanMap( map );
        return mapper.readValue( mapper.writeValueAsString( map ), JSONSchemaProps.class );
    }

    @SuppressWarnings("unchecked")
    private void cleanMap( Map<String, Object> map )
    {
        map.remove( "extends" );
        map.remove( "javaName" );
        for (Object o : map.values()) {
            if (o instanceof Map) {
                cleanMap( (Map<String, Object>) o );
            }
        }
    }

    private void assertSchema( final JSONSchemaProps s1, final JSONSchemaProps s2 )
        throws JsonProcessingException
    {
        assertEquals( mapper.writerWithDefaultPrettyPrinter().writeValueAsString( s1 ),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString( s2 ) );
    }
}
