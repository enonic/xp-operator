package com.enonic.kubernetes.client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnableRuleMigrationSupport
public class SchemaTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Rule
    public KubernetesServer server = new KubernetesServer();

    @Test
    void v1Xp7App()
        throws IOException
    {
        CustomResourceDefinition customResourceDefinition = loadCRD( "/crds/apps.yaml" );
        JSONSchemaProps schema = loadSchema( "/schema/v1/xp7app/xp7App.json" );

        assertSchema( getSchemaVersion( customResourceDefinition, "v1" ), schema );
    }

    @Test
    void v1Domain()
        throws IOException
    {
        CustomResourceDefinition customResourceDefinition = loadCRD( "/crds/domains.yaml" );
        JSONSchemaProps schema = loadSchema( "/schema/v1/domain/domain.json" );

        assertSchema( getSchemaVersion( customResourceDefinition, "v1" ), schema );
    }

    @Test
    void v1Xp7Config()
        throws IOException
    {
        CustomResourceDefinition customResourceDefinition = loadCRD( "/crds/configs.yaml" );
        JSONSchemaProps schema = loadSchema( "/schema/v1/xp7config/xp7Config.json" );

        assertSchema( getSchemaVersion( customResourceDefinition, "v1" ), schema );
    }

    @Test
    void v1Xp7Deployment()
        throws IOException
    {
        CustomResourceDefinition customResourceDefinition = loadCRD( "/crds/deployments.yaml" );
        JSONSchemaProps schema = loadSchema( "/schema/v1/xp7deployment/xp7Deployment.json" );

        assertSchema( getSchemaVersion( customResourceDefinition, "v1" ), schema );
    }

    private CustomResourceDefinition loadCRD( final String file )
    {
        KubernetesClient client = server.getClient();
        CustomResourceDefinition customResourceDefinition = client
            .apiextensions()
            .v1()
            .customResourceDefinitions()
            .load( getClass().getResourceAsStream( file ) )
            .get();
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
