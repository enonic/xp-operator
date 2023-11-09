package com.enonic.kubernetes.operator.domain;

import com.enonic.kubernetes.client.DefaultEnonicKubernetesClient;
import com.enonic.kubernetes.client.EnonicKubernetesClient;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.ClientsImpl;

import io.fabric8.kubernetes.api.model.LoadBalancerIngressBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;

import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.arc.profile.IfBuildProfile;

import io.quarkus.test.common.QuarkusTestResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.ws.rs.Produces;

import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableKubernetesMockClient(crud = true)
public class LbServiceIpProducerTest
{
    private KubernetesClient kubernetesClient;

    private KubernetesMockServer server;

    private LbServiceIpProducer lbServiceIpProducer;

    private LBConfig config;

    @BeforeEach
    void setUp()
    {
        final EnonicKubernetesClient client = new DefaultEnonicKubernetesClient( kubernetesClient );
        final Clients clients =
            ClientsImpl.of( client.k8s(), client.enonic(), client.enonic().v1().crds().xp7apps(), client.enonic().v1().crds().xp7configs(),
                            client.enonic().v1().crds().xp7deployments(), client.enonic().v1().crds().domains() );

        lbServiceIpProducer = new LbServiceIpProducer();
        lbServiceIpProducer.clients = clients;

        config = mock( LBConfig.class );

        when( config.serviceName() ).thenReturn( "ingress-nginx-controller" );
        when( config.serviceNamespace() ).thenReturn( "ec-system" );

        lbServiceIpProducer.lbConfig = config;
    }

    @Test
    public void testGetLbIp_CnameConfigured()
    {
        when( config.cname() ).thenReturn( Optional.of( "cdn.enonic.com" ) );

        List<String> lbIps = lbServiceIpProducer.get();

        assertTrue( lbIps.isEmpty() );
    }

    @Test
    public void testGetLbIp_StaticIpConfigured()
    {
        when( config.staticIp() ).thenReturn( Optional.of( "34.88.83.183" ) );

        List<String> lbIps = lbServiceIpProducer.get();

        assertEquals( 1, lbIps.size() );
    }

    @Test
    public void testGetLbIp_StaticIpAndCnameConfigured()
    {
        when( config.staticIp() ).thenReturn( Optional.of( "34.88.83.183" ) );
        when( config.cname() ).thenReturn( Optional.of( "cdn.enonic.com" ) );

        assertThrows( RuntimeException.class, () -> lbServiceIpProducer.get() );
    }

    @Test
    public void testGetLbIp_invalidServiceName()
    {
        when( config.serviceName() ).thenReturn( "invalid-service-name" );
        lbServiceIpProducer.dnsEnabled = true;

        final List<String> lbIps = lbServiceIpProducer.get();
        assertEquals( 0, lbIps.size() );
    }

    @Test
    public void testGetLbIp()
    {
        final Service service = mock( Service.class, Mockito.RETURNS_DEEP_STUBS );
        when( service.getStatus().getLoadBalancer().getIngress() ).thenReturn(
            List.of( new LoadBalancerIngressBuilder().withIp( "1.2.3.4" ).build() ) );

        server.expect().get().withPath( "/api/v1/namespaces/ec-system/services/ingress-nginx-controller" ).andReturn( 200, service ).once();

        final List<String> lbIps = lbServiceIpProducer.get();

        assertEquals( 1, lbIps.size() );
        assertEquals( "1.2.3.4", lbIps.get( 0 ) );
    }

//    private IPLBConfig initConfig()
//    {
////        lbServiceIpProducer.lbConfig = new IPLBConfig();
//        final IPLBConfig config = (IPLBConfig) lbServiceIpProducer.lbConfig;
//
//        config.cname = null;
//        config.staticIp = null;
//        config.serviceName = "ingress-nginx-controller";
//
//        return config;
//    }

//    @Mock
//    public static class IPLBConfig
//        implements LBConfig
//    {
//
//        String cname;
//
//        String staticIp;
//
//        String serviceName;
//
//        @Override
//        public Optional<String> cname()
//        {
//            return Optional.ofNullable( cname );
//        }
//
//        @Override
//        public Optional<String> staticIp()
//        {
//            return Optional.ofNullable( staticIp );
//        }
//
//        @Override
//        public String serviceName()
//        {
//            return serviceName;
//        }
//
//        @Override
//        public String serviceNamespace()
//        {
//            return "ec-system";
//        }
//
//        @Override
//        public String domainWildcardCertificate()
//        {
//            return null;
//        }
//    }

}

