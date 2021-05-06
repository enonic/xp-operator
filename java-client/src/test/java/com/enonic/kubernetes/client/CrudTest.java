package com.enonic.kubernetes.client;

import com.enonic.kubernetes.client.v1alpha1.Xp7App;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppSpec;
import com.enonic.kubernetes.client.v1alpha2.Domain;
import com.enonic.kubernetes.client.v1alpha2.Xp7Config;
import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.client.v1alpha2.domain.DomainSpec;
import com.enonic.kubernetes.client.v1alpha2.domain.DomainSpecCertificate;
import com.enonic.kubernetes.client.v1alpha2.xp7config.Xp7ConfigSpec;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpec;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroupDisk;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroupEnvVar;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroupResources;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpecNodesSharedDisk;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@EnableRuleMigrationSupport
public class CrudTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Rule
    public KubernetesServer server = new KubernetesServer( false, true );

    @Test
    void v1alpha1Xp7App()
        throws IOException, URISyntaxException
    {
        KubernetesClient k8sClient = server.getClient();

        CustomResourceDefinition crd = k8sClient
            .apiextensions()
            .v1beta1()
            .customResourceDefinitions()
            .load( getClass().getResourceAsStream( "/crds/apps.yaml" ) )
            .get();

        k8sClient
            .apiextensions()
            .v1beta1()
            .customResourceDefinitions()
            .create( crd );

        // Create crd client
        MixedOperation<Xp7App, Xp7App.Xp7AppList, Resource<Xp7App>> crdClient = Xp7App.createCrdClient( k8sClient );

        // Create resource
        ObjectMetaBuilder metadataBuilder = new ObjectMetaBuilder().withName( "test-name" );
        Xp7App resource = new Xp7App().withSpec( new Xp7AppSpec().withUrl( "test" ) );
        resource.setMetadata( metadataBuilder.build() );
        assertCrd( resource, "/crud-app.json" );

        // Send to server
        crdClient.create( resource );

        // List
        Xp7App.Xp7AppList list = crdClient.list();
        assertNotNull( list );
        assertEquals( 1, list.getItems().size() );
        assertEqualsCrd( resource, list.getItems().get( 0 ) );

        // Fetch from server
        Xp7App get = crdClient.withName( "test-name" ).get();
        assertNotNull( get );
        assertEqualsCrd( resource, get );

        // Test put
        resource.setMetadata( metadataBuilder.withLabels( Map.of( "test2", "test2" ) ).build() );
        resource.setSpec( new Xp7AppSpec().withUrl( "test" ) );
        crdClient.withName( "test-name" ).replace( resource );

        // Delete from server
        assertTrue( crdClient.withName( "test-name" ).delete() );
    }

    @Test
    void v1alpha2Domain()
        throws IOException, URISyntaxException
    {
        KubernetesClient k8sClient = server.getClient();

        // Create crd
        CustomResourceDefinition crd = k8sClient
            .apiextensions()
            .v1beta1()
            .customResourceDefinitions()
            .load( getClass().getResourceAsStream( "/crds/domains.yaml" ) )
            .get();

        k8sClient
            .apiextensions()
            .v1beta1()
            .customResourceDefinitions()
            .create( crd );

        // Create crd client
        MixedOperation<Domain, Domain.DomainList, Resource<Domain>> crdClient = Domain.createCrdClient( k8sClient );

        // Create resource
        ObjectMetaBuilder metadataBuilder = new ObjectMetaBuilder().withName( "test-name" );
        Domain resource = new Domain().withSpec(
            new DomainSpec().withHost( "test.host.com" ).withCdn( true ).withDnsRecord( true ).withDomainSpecCertificate(
                new DomainSpecCertificate().withAuthority( DomainSpecCertificate.Authority.SELF_SIGNED ) ) );
        resource.setMetadata( metadataBuilder.build() );
        assertCrd( resource, "/crud-domain.json" );

        // Send to server
        crdClient.create( resource );

        // List
        Domain.DomainList list = crdClient.list();
        assertNotNull( list );
        assertEquals( 1, list.getItems().size() );
        assertEqualsCrd( resource, list.getItems().get( 0 ) );

        // Fetch from server
        Domain get = crdClient.withName( "test-name" ).get();
        assertNotNull( get );
        assertEquals( resource, get );

        // Test put
        resource.setMetadata( metadataBuilder.withLabels( Map.of( "test2", "test2" ) ).build() );
        resource.setSpec( resource.getSpec().withHost( "new.host.com" ) );
        crdClient.withName( "test-name" ).replace( resource );

        // Delete from server
        assertTrue( crdClient.withName( "test-name" ).delete() );
    }

    @Test
    void v1alpha2xp7Config()
        throws IOException, URISyntaxException
    {
        KubernetesClient k8sClient = server.getClient();

        // Create crd
        CustomResourceDefinition crd = k8sClient
            .apiextensions()
            .v1beta1()
            .customResourceDefinitions()
            .load( getClass().getResourceAsStream( "/crds/configs.yaml" ) )
            .get();

        k8sClient
            .apiextensions()
            .v1beta1()
            .customResourceDefinitions()
            .create( crd );

        // Create crd client
        MixedOperation<Xp7Config, Xp7Config.Xp7ConfigList, Resource<Xp7Config>> crdClient = Xp7Config.createCrdClient( k8sClient );

        // Create resource
        ObjectMetaBuilder metadataBuilder = new ObjectMetaBuilder().withName( "test-name" );
        Xp7Config resource =
            new Xp7Config().withSpec( new Xp7ConfigSpec().withData( "test" ).withFile( "test.cfg" ).withNodeGroup( "test" ) );
        resource.setMetadata( metadataBuilder.build() );
        assertCrd( resource, "/crud-config.json" );

        // Send to server
        crdClient.create( resource );

        // List
        Xp7Config.Xp7ConfigList list = crdClient.list();
        assertNotNull( list );
        assertEquals( 1, list.getItems().size() );
        assertEqualsCrd( resource, list.getItems().get( 0 ) );
        assertEquals( resource, list.getItems().get( 0 ) );

        // Fetch from server
        Xp7Config get = crdClient.withName( "test-name" ).get();
        assertNotNull( get );
        assertEquals( resource, get );

        // Test put
        resource.setMetadata( metadataBuilder.withLabels( Map.of( "test2", "test2" ) ).build() );
        resource.setSpec( new Xp7ConfigSpec().withData( "test2" ).withFile( "test2.cfg" ).withNodeGroup( "test2" ) );
        crdClient.withName( "test-name" ).replace( resource );

        // Delete from server
        assertTrue( crdClient.withName( "test-name" ).delete() );
    }

    @Test
    void v1alpha2Xp7Deployment()
        throws IOException, URISyntaxException
    {
        KubernetesClient k8sClient = server.getClient();

        // Create crd
        CustomResourceDefinition crd = k8sClient
            .apiextensions()
            .v1beta1()
            .customResourceDefinitions()
            .load( getClass().getResourceAsStream( "/crds/deployments.yaml" ) )
            .get();

        k8sClient
            .apiextensions()
            .v1beta1()
            .customResourceDefinitions()
            .create( crd );

        // Create crd client
        MixedOperation<Xp7Deployment, Xp7Deployment.Xp7DeploymentList, Resource<Xp7Deployment>> crdClient =
            Xp7Deployment.createCrdClient( k8sClient );

        // Create resource
        ObjectMetaBuilder metadataBuilder = new ObjectMetaBuilder().withName( "test-name" );
        Xp7Deployment resource = new Xp7Deployment()
            .withSpec( new Xp7DeploymentSpec()
                .withEnabled( true )
                .withXpVersion( "unstable" )
                .withXp7DeploymentSpecNodesSharedDisks(
                    Collections.singletonList( new Xp7DeploymentSpecNodesSharedDisk()
                        .withName( "blobstore" )
                        .withSize( "150" ) ) )
                .withXp7DeploymentSpecNodeGroups(
                    Collections.singletonList(
                        new Xp7DeploymentSpecNodeGroup()
                            .withName( "test" )
                            .withDisplayName( "test" )
                            .withReplicas( 1 )
                            .withData( true )
                            .withMaster( true )
                            .withXp7DeploymentSpecNodeGroupEnvironment( Collections.singletonList(
                                new Xp7DeploymentSpecNodeGroupEnvVar()
                                    .withName( "test" )
                                    .withValue( "test" ) ) )
                            .withXp7DeploymentSpecNodeGroupResources(
                                new Xp7DeploymentSpecNodeGroupResources()
                                    .withCpu( "1" )
                                    .withMemory( "512Mi" )
                                    .withXp7DeploymentSpecNodeGroupDisks(
                                        Collections.singletonList(
                                            new Xp7DeploymentSpecNodeGroupDisk()
                                                .withName( "index" )
                                                .withSize( "100m" ) ) ) ) ) ) );
        resource.setMetadata( metadataBuilder.build() );
        assertCrd( resource, "/crud-deployment.json" );

        // Send to server
        crdClient.create( resource );

        // List
        Xp7Deployment.Xp7DeploymentList list = crdClient.list();
        assertNotNull( list );
        assertEquals( 1, list.getItems().size() );
        assertEqualsCrd( resource, list.getItems().get( 0 ) );

        // Fetch from server
        Xp7Deployment get = crdClient.withName( "test-name" ).get();
        assertNotNull( get );
        assertEquals( resource, get );

        // Test put
        resource.setMetadata( metadataBuilder.withLabels( Map.of( "test2", "test2" ) ).build() );
        resource.setSpec( resource.getSpec().withEnabled( false ) );
        crdClient.withName( "test-name" ).replace( resource );

        // Delete from server
        assertTrue( crdClient.withName( "test-name" ).delete() );
    }

    private void assertContext( final CustomResourceDefinition customResourceDefinition,
                                final CustomResourceDefinitionContext customResourceDefinitionContext )
    {
        String version = null;
        for (CustomResourceDefinitionVersion v : customResourceDefinition.getSpec().getVersions()) {
            if (v.getName().equals( customResourceDefinitionContext.getVersion() )) {
                version = v.getName();
            }
        }

        Assert.assertEquals(
            String.format( "%s.%s",
                customResourceDefinition.getSpec().getNames().getPlural(),
                customResourceDefinition.getSpec().getGroup() ),
            customResourceDefinitionContext.getName() );

        Assert.assertEquals(
            customResourceDefinition.getSpec().getGroup(),
            customResourceDefinitionContext.getGroup() );

        Assert.assertEquals(
            customResourceDefinition.getSpec().getScope(),
            customResourceDefinitionContext.getScope() );

        Assert.assertEquals(
            customResourceDefinition.getSpec().getNames().getPlural(),
            customResourceDefinitionContext.getPlural() );

        Assert.assertEquals(
            customResourceDefinition.getSpec().getVersions().get( 0 ).getName(),
            customResourceDefinitionContext.getVersion() );

        Assert.assertEquals(
            customResourceDefinition.getSpec().getNames().getKind(),
            customResourceDefinitionContext.getKind() );

        Assert.assertEquals(
            version,
            customResourceDefinitionContext.getVersion() );
    }

    void assertCrd( CustomResource crd, String file )
        throws IOException, URISyntaxException
    {
        assertEquals( Files.readString( Path.of( getClass().getResource( file ).toURI() ) ),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString( crd ) + "\n" );
    }

    private void assertEqualsCrd( final CustomResource expected, final CustomResource actual )
    {
        expected.getMetadata().setCreationTimestamp( actual.getMetadata().getCreationTimestamp() );
        expected.getMetadata().setGeneration( actual.getMetadata().getGeneration() );
        expected.getMetadata().setResourceVersion( actual.getMetadata().getResourceVersion() );
        expected.getMetadata().setUid( actual.getMetadata().getUid() );
        assertEquals( expected, actual );
    }
}
