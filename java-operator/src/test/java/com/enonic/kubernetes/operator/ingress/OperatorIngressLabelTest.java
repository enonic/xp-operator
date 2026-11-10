package com.enonic.kubernetes.operator.ingress;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorIngressLabelTest
{
    private static final Map<String, String> MAPPING = Map.of( "enonic.cloud/xp7.vhost.mapping.1.source", "/" );

    @Test
    void anIngressNotLoadedYetAwaitsItsVhost()
    {
        assertTrue( OperatorIngressLabel.awaitsVhost( ingress( MAPPING, Map.of( "enonic.cloud/vhost-loaded", "false" ) ) ) );
    }

    @Test
    void aLoadedIngressDoesNot()
    {
        assertFalse( OperatorIngressLabel.awaitsVhost( ingress( MAPPING, Map.of( "enonic.cloud/vhost-loaded", "true" ) ) ) );
    }

    // The label edit on create fails until the operator's role exists in a new namespace.
    @Test
    void aMappedIngressThatWasNeverLabelledAwaitsItsVhost()
    {
        assertTrue( OperatorIngressLabel.awaitsVhost( ingress( MAPPING, null ) ) );
    }

    @Test
    void anIngressWithoutAMappingDoesNot()
    {
        assertFalse( OperatorIngressLabel.awaitsVhost( ingress( Map.of( "enonic.cloud/ingress.cert.manage", "true" ), null ) ) );
        assertFalse( OperatorIngressLabel.awaitsVhost( ingress( null, null ) ) );
    }

    private static Ingress ingress( final Map<String, String> annotations, final Map<String, String> labels )
    {
        return new IngressBuilder().withNewMetadata().withName( "admin" ).withAnnotations( annotations ).withLabels( labels ).endMetadata().build();
    }
}
