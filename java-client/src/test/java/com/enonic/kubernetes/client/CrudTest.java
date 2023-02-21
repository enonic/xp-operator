package com.enonic.kubernetes.client;

import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.client.v1.domain.DomainSpec;
import com.enonic.kubernetes.client.v1.domain.DomainSpecCertificate;
import com.enonic.kubernetes.client.v1.xp7app.Xp7App;
import com.enonic.kubernetes.client.v1.xp7app.Xp7AppSpec;
import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.client.v1.xp7config.Xp7ConfigSpec;
import com.enonic.kubernetes.client.v1.xp7deployment.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;


@EnableRuleMigrationSupport
public class CrudTest
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Rule
    public KubernetesServer server = new KubernetesServer( false, true );

    @Test
    void v1Xp7App()
        throws IOException, URISyntaxException
    {
        final EnonicKubernetesClient client = new DefaultEnonicKubernetesClient( server.getClient() );

        final NonNamespaceOperation<CustomResourceDefinition, CustomResourceDefinitionList, Resource<CustomResourceDefinition>>
            customResourceDefinitions = client.k8s().apiextensions().v1().customResourceDefinitions();

        final CustomResourceDefinition crd = customResourceDefinitions.load( getClass().getResourceAsStream( "/crds/apps.yaml" ) ).item();

        customResourceDefinitions.resource( crd ).create();

        // Create crd client
        final MixedOperation<Xp7App, Xp7App.Xp7AppList, Resource<Xp7App>> crdClient = client.enonic().v1().crds().xp7apps();

        // Create resource
        final ObjectMetaBuilder metadataBuilder = new ObjectMetaBuilder().withNamespace( "test" ).withName( "test-name" );
        final Xp7App resource = new Xp7App().withSpec( new Xp7AppSpec().withUrl( "test" ) );
        resource.setMetadata( metadataBuilder.build() );
        assertCrd( resource, "/crud-app.json" );

        // Send to server
        crdClient.resource( resource ).create();

        // List
        final Xp7App.Xp7AppList list = crdClient.list();
        assertNotNull( list );
        assertEquals( 1, list.getItems().size() );
        assertEqualsCrd( resource, list.getItems().get( 0 ) );

        // Fetch from server
        final Xp7App get = crdClient.withName( "test-name" ).get();
        assertNotNull( get );
        assertEqualsCrd( resource, get );

        // Test put
        resource.setMetadata( metadataBuilder.withLabels( Map.of( "test2", "test2" ) ).build() );
        resource.setSpec( new Xp7AppSpec().withUrl( "test" ) );
        crdClient.withName( "test-name" ).replace( resource );

        // Delete from server
        assertFalse( crdClient.withName( "test-name" ).delete().isEmpty() );
    }

    @Test
    void v1Domain()
        throws IOException, URISyntaxException
    {
        final EnonicKubernetesClient client = new DefaultEnonicKubernetesClient( server.getClient() );

        // Create crd
        final CustomResourceDefinition crd = client.k8s()
            .apiextensions()
            .v1()
            .customResourceDefinitions()
            .load( getClass().getResourceAsStream( "/crds/domains.yaml" ) )
            .item();

        client.k8s().apiextensions().v1().customResourceDefinitions().resource( crd ).create();

        // Create crd client
        final MixedOperation<Domain, Domain.DomainList, Resource<Domain>> crdClient = client.enonic().v1().crds().domains();

        // Create resource
        final ObjectMetaBuilder metadataBuilder = new ObjectMetaBuilder().withName( "test-name" );
        final Domain resource = new Domain().withSpec( new DomainSpec().withHost( "test.host.com" )
                                                           .withCdn( true )
                                                           .withDnsRecord( true )
                                                           .withDomainSpecCertificate( new DomainSpecCertificate().withAuthority(
                                                               DomainSpecCertificate.Authority.SELF_SIGNED ) ) );
        resource.setMetadata( metadataBuilder.build() );
        assertCrd( resource, "/crud-domain.json" );

        // Send to server
        crdClient.resource( resource ).create();

        // List
        final Domain.DomainList list = crdClient.list();
        assertNotNull( list );
        assertEquals( 1, list.getItems().size() );
        assertEqualsCrd( resource, list.getItems().get( 0 ) );

        // Fetch from server
        final Domain get = crdClient.withName( "test-name" ).get();
        assertNotNull( get );
        assertEquals( resource, get );

        // Test put
        resource.setMetadata( metadataBuilder.withLabels( Map.of( "test2", "test2" ) ).build() );
        resource.setSpec( resource.getSpec().withHost( "new.host.com" ) );
        crdClient.withName( "test-name" ).replace( resource );

        // Delete from server
        assertFalse( crdClient.withName( "test-name" ).delete().isEmpty() );
    }

    @Test
    void v1xp7Config()
        throws IOException, URISyntaxException
    {
        final EnonicKubernetesClient client = new DefaultEnonicKubernetesClient( server.getClient() );

        // Create crd
        final CustomResourceDefinition crd = client.k8s()
            .apiextensions()
            .v1()
            .customResourceDefinitions()
            .load( getClass().getResourceAsStream( "/crds/configs.yaml" ) )
            .item();

        client.k8s().apiextensions().v1().customResourceDefinitions().resource( crd ).create();

        // Create crd client
        final MixedOperation<Xp7Config, Xp7Config.Xp7ConfigList, Resource<Xp7Config>> crdClient = client.enonic().v1().crds().xp7configs();

        // Create resource
        final ObjectMetaBuilder metadataBuilder = new ObjectMetaBuilder().withNamespace( "test" ).withName( "test-name" );
        final Xp7Config resource =
            new Xp7Config().withSpec( new Xp7ConfigSpec().withData( "test" ).withFile( "test.cfg" ).withNodeGroup( "test" ) );
        resource.setMetadata( metadataBuilder.build() );
        assertCrd( resource, "/crud-config.json" );

        // Send to server
        crdClient.resource( resource ).create();

        // List
        final Xp7Config.Xp7ConfigList list = crdClient.list();
        assertNotNull( list );
        assertEquals( 1, list.getItems().size() );
        assertEqualsCrd( resource, list.getItems().get( 0 ) );
        assertEquals( resource, list.getItems().get( 0 ) );

        // Fetch from server
        final Xp7Config get = crdClient.withName( "test-name" ).get();
        assertNotNull( get );
        assertEquals( resource, get );

        // Test put
        resource.setMetadata( metadataBuilder.withLabels( Map.of( "test2", "test2" ) ).build() );
        resource.setSpec( new Xp7ConfigSpec().withData( "test2" ).withFile( "test2.cfg" ).withNodeGroup( "test2" ) );
        crdClient.withName( "test-name" ).replace( resource );

        // Delete from server
        assertFalse( crdClient.withName( "test-name" ).delete().isEmpty() );
    }

    @Test
    void v1Xp7Deployment()
        throws IOException, URISyntaxException
    {
        EnonicKubernetesClient client = new DefaultEnonicKubernetesClient( server.getClient() );

        // Create crd
        CustomResourceDefinition crd = client.k8s()
            .apiextensions()
            .v1()
            .customResourceDefinitions()
            .load( getClass().getResourceAsStream( "/crds/deployments.yaml" ) )
            .item();

        client.k8s().apiextensions().v1().customResourceDefinitions().resource( crd ).create();

        // Create crd client
        MixedOperation<Xp7Deployment, Xp7Deployment.Xp7DeploymentList, Resource<Xp7Deployment>> crdClient =
            client.enonic().v1().crds().xp7deployments();

        // Create resource
        ObjectMetaBuilder metadataBuilder = new ObjectMetaBuilder().withNamespace( "test" ).withName( "test-name" );
        Xp7Deployment resource = new Xp7Deployment().withSpec( new Xp7DeploymentSpec().withEnabled( true )
                                                                   .withXpVersion( "unstable" )
                                                                   .withXp7DeploymentSpecNodesSharedDisks( Collections.singletonList(
                                                                       new Xp7DeploymentSpecNodesSharedDisk().withName( "blobstore" )
                                                                           .withSize( "150" ) ) )
                                                                   .withXp7DeploymentSpecNodeGroups( Collections.singletonList(
                                                                       new Xp7DeploymentSpecNodeGroup().withName( "test" )
                                                                           .withDisplayName( "test" )
                                                                           .withReplicas( 1 )
                                                                           .withData( true )
                                                                           .withMaster( true )
                                                                           .withXp7DeploymentSpecNodeGroupEnvironment(
                                                                               Collections.singletonList(
                                                                                   new Xp7DeploymentSpecNodeGroupEnvVar().withName( "test" )
                                                                                       .withValue( "test" ) ) )
                                                                           .withXp7DeploymentSpecNodeGroupResources(
                                                                               new Xp7DeploymentSpecNodeGroupResources().withCpu( "1" )
                                                                                   .withMemory( "512Mi" )
                                                                                   .withXp7DeploymentSpecNodeGroupDisks(
                                                                                       Collections.singletonList(
                                                                                           new Xp7DeploymentSpecNodeGroupDisk().withName(
                                                                                               "index" ).withSize( "100m" ) ) ) ) ) ) );
        resource.setMetadata( metadataBuilder.build() );
        assertCrd( resource, "/crud-deployment.json" );

        // Send to server
        crdClient.resource( resource ).create();

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
        assertFalse( crdClient.withName( "test-name" ).delete().isEmpty() );
    }

    private void assertContext( final CustomResourceDefinition customResourceDefinition,
                                final CustomResourceDefinitionContext customResourceDefinitionContext )
    {
        String version = null;
        for ( CustomResourceDefinitionVersion v : customResourceDefinition.getSpec().getVersions() )
        {
            if ( v.getName().equals( customResourceDefinitionContext.getVersion() ) )
            {
                version = v.getName();
            }
        }

        assertEquals( String.format( "%s.%s", customResourceDefinition.getSpec().getNames().getPlural(),
                                     customResourceDefinition.getSpec().getGroup() ), customResourceDefinitionContext.getName() );

        assertEquals( customResourceDefinition.getSpec().getGroup(), customResourceDefinitionContext.getGroup() );

        assertEquals( customResourceDefinition.getSpec().getScope(), customResourceDefinitionContext.getScope() );

        assertEquals( customResourceDefinition.getSpec().getNames().getPlural(), customResourceDefinitionContext.getPlural() );

        assertEquals( customResourceDefinition.getSpec().getVersions().get( 0 ).getName(), customResourceDefinitionContext.getVersion() );

        assertEquals( customResourceDefinition.getSpec().getNames().getKind(), customResourceDefinitionContext.getKind() );

        assertEquals( version, customResourceDefinitionContext.getVersion() );
    }

    void assertCrd( CustomResource<?, ?> crd, String file )
        throws IOException, URISyntaxException
    {
        Path path = Path.of( Objects.requireNonNull( getClass().getResource( file ) ).toURI() );
        assertEquals( Files.readString( path ), mapper.writerWithDefaultPrettyPrinter().writeValueAsString( crd ) + "\n" );
    }

    private void assertEqualsCrd( final CustomResource<?, ?> expected, final CustomResource<?, ?> actual )
    {
        expected.getMetadata().setCreationTimestamp( actual.getMetadata().getCreationTimestamp() );
        expected.getMetadata().setGeneration( actual.getMetadata().getGeneration() );
        expected.getMetadata().setResourceVersion( actual.getMetadata().getResourceVersion() );
        expected.getMetadata().setUid( actual.getMetadata().getUid() );
        assertEquals( expected, actual );
    }
}
