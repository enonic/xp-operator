package com.enonic.kubernetes.operator.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//@ExtendWith(MockitoExtension.class)
@QuarkusTest
public class LbServiceIpProducerTest
{
    @Inject
    LbServiceIpProducer lbServiceIpProducer;

    @BeforeEach
    void setUp()
    {
    }

    @Test
    public void testGetLbIp_CnameConfigured()
    {
        initConfig();

        final IPLBConfig config = (IPLBConfig) lbServiceIpProducer.lbConfig;
        config.cname = "cdn.enonic.com";

        List<String> lbIps = lbServiceIpProducer.get();

        assertTrue( lbIps.isEmpty() );
    }

    @Test
    public void testGetLbIp_StaticIpConfigured()
    {
        initConfig();

        final IPLBConfig config = (IPLBConfig) lbServiceIpProducer.lbConfig;
        config.staticIp = "1.2.3.4";

        List<String> lbIps = lbServiceIpProducer.get();

        assertEquals( 1, lbIps.size() );
    }

    @Test
    public void testGetLbIp_StaticIpAndCnameConfigured()
    {
        final IPLBConfig config = initConfig();

        config.staticIp = "1.2.3.4";
        config.cname = "cdn.enonic.com";

        assertThrows( RuntimeException.class, () -> lbServiceIpProducer.get() );
    }

    @Test
    public void testGetLbIp_invalidServiceName()
    {
        final IPLBConfig config = initConfig();
        config.serviceName = "invalid-service-name";

        final List<String> lbIps = lbServiceIpProducer.get();
        assertEquals( 0, lbIps.size() );
    }

    @Test
    public void testGetLbIp()
    {
        initConfig();

        final List<String> lbIps = lbServiceIpProducer.get();
        assertEquals( 1, lbIps.size() );
    }

    private IPLBConfig initConfig()
    {
        final IPLBConfig config = (IPLBConfig) lbServiceIpProducer.lbConfig;
        config.cname = null;
        config.staticIp = null;
        config.serviceName = "ingress-nginx-controller";

        return config;
    }


    @Mock
    public static class IPLBConfig
        implements LBConfig
    {

        String cname;

        String staticIp;

        String serviceName;

        @Override
        public Optional<String> cname()
        {
            return Optional.ofNullable( cname );
        }

        @Override
        public Optional<String> staticIp()
        {
            return Optional.ofNullable( staticIp );
        }

        @Override
        public String serviceName()
        {
            return serviceName;
        }

        @Override
        public String serviceNamespace()
        {
            return "ec-system";
        }
    }

}

