package com.enonic.kubernetes.operator.domain;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;

import com.enonic.kubernetes.client.CustomClient;
import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.client.v1.xp7app.Xp7App;
import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LbServiceIpProducerTest1
{
//    LbServiceIpProducer lbServiceIpProducer;
//
//    @BeforeEach
//    void setUp()
//    {
//        lbServiceIpProducer = new LbServiceIpProducer();
//
//        lbServiceIpProducer.clients = Mockito.mock( Clients.class );
//        ;
//        lbServiceIpProducer.lbConfig = new IPLBConfig();
//        lbServiceIpProducer.dnsEnabled = true;
//    }
//
//    @Test
//    public void testGetLbIp_CnameConfigured()
//    {
//        initConfig();
//
//        final IPLBConfig config = (IPLBConfig) lbServiceIpProducer.lbConfig;
//        config.cname = "cdn.enonic.com";
//
//        List<String> lbIps = lbServiceIpProducer.get();
//
//        assertTrue( lbIps.isEmpty() );
//    }
//
//    @Test
//    public void testGetLbIp_StaticIpConfigured()
//    {
//        final IPLBConfig config = initConfig();
//        config.staticIp = "1.2.3.4";
//
//        List<String> lbIps = lbServiceIpProducer.get();
//
//        assertEquals( 1, lbIps.size() );
//    }
//
//    @Test
//    public void testGetLbIp_StaticIpAndCnameConfigured()
//    {
//        final IPLBConfig config = initConfig();
//
//        config.staticIp = "1.2.3.4";
//        config.cname = "cdn.enonic.com";
//
//        assertThrows( RuntimeException.class, () -> lbServiceIpProducer.get() );
//    }
//
//    @Test
//    public void testGetLbIp_invalidServiceName()
//    {
//        final IPLBConfig config = initConfig();
//        config.serviceName = "invalid-service-name";
//
//        final KubernetesClient kubernetesClient = Mockito.mock( KubernetesClient.class );
//        final MixedOperation mixedOperation = Mockito.mock( MixedOperation.class );
//        final io.fabric8.kubernetes.client.dsl.ServiceResource service = Mockito.mock( io.fabric8.kubernetes.client.dsl.ServiceResource.class );
//
//        Mockito.when( lbServiceIpProducer.clients.k8s() ).thenReturn( kubernetesClient );
//        Mockito.when( kubernetesClient.services() ).thenReturn( mixedOperation );
//        Mockito.when( mixedOperation.inNamespace( ArgumentMatchers.any() ) ).thenReturn( mixedOperation );
//        Mockito.when( mixedOperation.withName( ArgumentMatchers.any() ) ).thenReturn( service );
//
//        final List<String> lbIps = lbServiceIpProducer.get();
//        assertEquals( 0, lbIps.size() );
//    }
//
//    @Test
//    public void testGetLbIp()
//    {
//        initConfig();
//
//        final KubernetesClient kubernetesClient = Mockito.mock( KubernetesClient.class );
//        final MixedOperation mixedOperation = Mockito.mock( MixedOperation.class );
//        final ServiceResource serviceResource = Mockito.mock( ServiceResource.class );
//        final Service service = Mockito.mock( Service.class );
//
//        Mockito.when( lbServiceIpProducer.clients.k8s() ).thenReturn( kubernetesClient );
//        Mockito.when( kubernetesClient.services() ).thenReturn( mixedOperation );
//        Mockito.when( mixedOperation.inNamespace( ArgumentMatchers.any() ) ).thenReturn( mixedOperation );
//        Mockito.when( mixedOperation.withName( ArgumentMatchers.any() ) ).thenReturn( serviceResource );
//        Mockito.when( service.getget() ).thenReturn( serviceResource );
//
//        final List<String> lbIps = lbServiceIpProducer.get();
//        assertEquals( 1, lbIps.size() );
//    }
//
//    private IPLBConfig initConfig()
//    {
//        final IPLBConfig config = (IPLBConfig) lbServiceIpProducer.lbConfig;
//        config.cname = null;
//        config.staticIp = null;
//        config.serviceName = "ingress-nginx-controller";
//
//        return config;
//    }
//
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
//    }

}

