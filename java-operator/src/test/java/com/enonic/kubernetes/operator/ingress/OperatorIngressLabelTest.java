package com.enonic.kubernetes.operator.ingress;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.crd.v1.Xp8ConfigSpec;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.operator.api.TestInformerSearcher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class OperatorIngressLabelTest
{
    private static final Map<String, String> MAPPING = Map.of( "enonic.cloud/xp8.vhost.mapping.1.source", "/" );

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

    @Test
    void anUpdatedVhostConfigWithoutStatusIsNotLoaded()
    {
        final OperatorIngressLabel operator = new OperatorIngressLabel();

        assertDoesNotThrow( () -> operator.onUpdate( vhostConfig( "1" ), vhostConfig( "2" ) ) );
    }

    @Test
    void anIngressIsNotLabelledLoadedWhileItsVhostConfigHasNoStatus()
    {
        final TestInformerSearcher<Ingress> ingresses = new TestInformerSearcher<>();
        ingresses.add( new IngressBuilder( ingress( MAPPING, null ) ).editMetadata()
                           .withNamespace( "ns" )
                           .endMetadata()
                           .withNewSpec()
                           .addNewRule()
                           .withHost( "env.example.com" )
                           .withNewHttp()
                           .addNewPath()
                           .withPath( "/" )
                           .withNewBackend()
                           .withNewService()
                           .withName( "main" )
                           .withNewPort()
                           .withNumber( 8080 )
                           .endPort()
                           .endService()
                           .endBackend()
                           .endPath()
                           .endHttp()
                           .endRule()
                           .endSpec()
                           .build() );
        final TestInformerSearcher<Xp8Config> configs = new TestInformerSearcher<>();
        configs.add( vhostConfig( "1" ) );

        final OperatorIngressLabel operator = new OperatorIngressLabel();
        operator.searchers = new Searchers( null, ingresses, null, null, configs, null, null );
        operator.clients = mock( Clients.class );

        assertDoesNotThrow( operator::run );
        verifyNoInteractions( operator.clients );
    }

    private static Xp8Config vhostConfig( final String resourceVersion )
    {
        final Xp8Config config = new Xp8Config();
        config.setMetadata( new ObjectMetaBuilder().withName( "main-vhosts" ).withNamespace( "ns" ).withResourceVersion( resourceVersion ).build() );
        final Xp8ConfigSpec spec = new Xp8ConfigSpec();
        spec.setNodeGroup( "main" );
        spec.setFile( "com.enonic.xp.web.vhost.cfg" );
        spec.setData( "" );
        config.setSpec( spec );
        return config;
    }

    private static Ingress ingress( final Map<String, String> annotations, final Map<String, String> labels )
    {
        return new IngressBuilder().withNewMetadata().withName( "admin" ).withAnnotations( annotations ).withLabels( labels ).endMetadata().build();
    }
}
