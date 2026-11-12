package com.enonic.kubernetes.operator.ingress;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorXp8ConfigSyncTest
{
    private static final String P = "enonic.cloud/xp8.vhost.mapping.1.";

    @Test
    void webMapping()
    {
        Mapping m = single( Map.of( P + "host", "env.example.com", P + "source", "/", P + "target", "/admin", P + "idproviders",
                                    "enonic-cloud,system" ) );

        assertEquals( 8080, m.port() );
        assertTrue( m.appliesTo( "/", 8080 ) );
        assertFalse( m.appliesTo( "/other", 8080 ) );
        assertFalse( m.appliesTo( "/", 4848 ) );
        assertFalse( m.appliesTo( "/", null ) );
        assertEquals( """


                          mapping.admin-1.host=env.example.com
                          mapping.admin-1.source=/
                          mapping.admin-1.target=/admin
                          mapping.admin-1.idProvider.enonic-cloud=default
                          mapping.admin-1.idProvider.system=enabled""", render( m ) );
    }

    @Test
    void managementMapping()
    {
        Mapping m = single( Map.of( P + "host", "env-mgmt.example.com", P + "source", "/", P + "target", "/", P + "idproviders",
                                    "enonic-cloud", P + "endpoint", "management", P + "flows.enonic-cloud", "autologin,k8s",
                                    P + "allow", "role:system.admin" ) );

        assertEquals( 4848, m.port() );
        assertTrue( m.appliesTo( "/server:app/", 4848 ) );
        assertTrue( m.appliesTo( "/server:project/", 4848 ) );
        assertFalse( m.appliesTo( "/server:app/", 8080 ) );
        assertEquals( """


                          mapping.admin-1.host=env-mgmt.example.com
                          mapping.admin-1.source=/
                          mapping.admin-1.target=/
                          mapping.admin-1.endpoint=management
                          mapping.admin-1.idProvider.enonic-cloud=default
                          mapping.admin-1.idProvider.enonic-cloud.flows=autologin,k8s
                          mapping.admin-1.allow=role:system.admin""", render( m ) );
    }

    @Test
    void unknownEndpointHasNoPort()
    {
        Mapping m = single( Map.of( P + "source", "/", P + "target", "/", P + "endpoint", "statistics" ) );

        assertEquals( -1, m.port() );
        assertFalse( m.appliesTo( "/", 4848 ) );
        assertFalse( m.appliesTo( "/", 8080 ) );
    }

    private static Mapping single( final Map<String, String> annotations )
    {
        Ingress ingress = new IngressBuilder().withNewMetadata().withName( "admin" ).withAnnotations( annotations ).endMetadata().build();
        Set<Mapping> mappings = OperatorXp8ConfigSync.getAnnotationMappings( ingress );
        assertEquals( 1, mappings.size() );
        return mappings.iterator().next();
    }

    private static String render( final Mapping m )
    {
        StringBuilder sb = new StringBuilder();
        OperatorXp8ConfigSync.addVHostMappings( sb, m );
        return sb.toString();
    }
}
