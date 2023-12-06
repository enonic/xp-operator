package com.enonic.kubernetes.operator.ingress;

import java.io.File;
import java.util.ServiceLoader;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;

import com.enonic.kubernetes.client.DefaultEnonicKubernetesClient;
import com.enonic.kubernetes.client.EnonicKubernetesClient;
import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.ClientsImpl;
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.SearchersImpl;
import com.enonic.kubernetes.operator.Operator;
import com.enonic.kubernetes.operator.api.TestInformerSearcher;
import com.enonic.kubernetes.testutils.TestFileSupplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableKubernetesMockClient(crud = true)
public class OperatorXp7ConfigSyncTest
{
    OperatorXp7ConfigSync operatorXp7ConfigSync;

    private TestInformerSearcher<Xp7Config> xp7ConfigInformerSearcher;

    private TestInformerSearcher<Ingress> xp7IngressInformerSearcher;

    private TestInformerSearcher<ConfigMap> xp7ConfigMapInformerSearcher;

    private EnonicKubernetesClient xpClient;

    private KubernetesClient kubernetesClient;

    @BeforeEach
    void setUp()
    {
        xpClient = new DefaultEnonicKubernetesClient( kubernetesClient );

        final Clients clients = ClientsImpl.of( xpClient.k8s(), xpClient.enonic(), xpClient.enonic().v1().crds().xp7apps(),
                                                xpClient.enonic().v1().crds().xp7configs(), xpClient.enonic().v1().crds().xp7deployments(),
                                                xpClient.enonic().v1().crds().domains() );

        xp7ConfigMapInformerSearcher = new TestInformerSearcher<>();
        xp7ConfigInformerSearcher = new TestInformerSearcher<>();
        xp7IngressInformerSearcher = new TestInformerSearcher<>();

        final TestInformerSearcher emptyInformerSearcher = new TestInformerSearcher();

        final Searchers searchers =
            SearchersImpl.of( xp7ConfigMapInformerSearcher, xp7IngressInformerSearcher, emptyInformerSearcher, emptyInformerSearcher,
                              emptyInformerSearcher, xp7ConfigInformerSearcher, emptyInformerSearcher, emptyInformerSearcher,
                              emptyInformerSearcher );

        operatorXp7ConfigSync = new OperatorXp7ConfigSync();

        operatorXp7ConfigSync.clients = clients;
        operatorXp7ConfigSync.searchers = searchers;
        operatorXp7ConfigSync.operator = new Operator();
    }

    @Test
    public void xp7ConfigSynchronized()
        throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper( new YAMLFactory() );

        final TestFileSupplier testFileSupplier = new TestFileSupplier();

        final File configFile = testFileSupplier.getFile( OperatorXp7ConfigSyncTest.class, "config.yaml" );
        final File ingressFile = testFileSupplier.getFile( OperatorXp7ConfigSyncTest.class, "ingress.yaml" );
        final File configMapFile = testFileSupplier.getFile( OperatorXp7ConfigSyncTest.class, "configMap.yaml" );
        final Xp7Config config = mapper.readValue( configFile, new TypeReference<>()
        {
        } );
        final Ingress ingress = mapper.readValue( ingressFile, new TypeReference<>()
        {
        } );
        final ConfigMap configMap = mapper.readValue( configMapFile, new TypeReference<>()
        {
        } );

        xp7ConfigInformerSearcher.add( config );
        xp7IngressInformerSearcher.add( ingress );
        xp7ConfigMapInformerSearcher.add( configMap );

        xpClient.enonic().v1().crds().xp7configs().resource( config ).create();

        operatorXp7ConfigSync.run();

        final Xp7Config updatedConfig = xpClient.enonic()
            .v1()
            .crds()
            .xp7configs()
            .inNamespace( config.getMetadata().getNamespace() )
            .withName( config.getMetadata().getName() )
            .get();

        assertTrue( config.getSpec().getData().contains( "my = custom" ) );
        assertFalse( config.getSpec().getData().contains( "mapping.localhost.host = localhost" ) );
        assertFalse( config.getSpec().getData().contains( "mapping.my-domain-com-my-mapping-admin.host=my-domain.com" ) );
        assertFalse( config.getSpec().getData().contains( "mapping.my-domain-com-my-mapping-site.host=my-domain.com" ) );

        assertFalse( updatedConfig.getSpec().getData().contains( "my = custom" ) );
        assertTrue( updatedConfig.getSpec().getData().contains( "mapping.localhost.host = localhost" ) );
        assertTrue( updatedConfig.getSpec().getData().contains( "mapping.my-domain-com-my-mapping-admin.host=my-domain.com" ) );
        assertTrue( updatedConfig.getSpec().getData().contains( "mapping.my-domain-com-my-mapping-site.host=my-domain.com" ) );
    }

}
