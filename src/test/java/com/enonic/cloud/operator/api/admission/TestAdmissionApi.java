package com.enonic.cloud.operator.api.admission;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.testutils.TestFileSupplier;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;

class TestAdmissionApi
    extends AdmissionApi
{
    private final TestXp7DeploymentCache deploymentCache;

    private final TestXp7VHostCache vHostCache;

    TestAdmissionApi()
    {
        mapper = new ObjectMapper( new YAMLFactory() );
        allNodesPicker = cfgStr( "operator.helm.charts.Values.allNodesKey" );
        deploymentCache = new TestXp7DeploymentCache();
        vHostCache = new TestXp7VHostCache();
        caches = new Caches( null, null, null, deploymentCache, null, null, vHostCache );
    }

    @TestFactory
    Stream<DynamicTest> tests()
        throws IOException
    {
        TestFileSupplier testFileSupplier = new TestFileSupplier();

        // Add deployments to cache
        File cache = testFileSupplier.getFile( TestAdmissionApi.class, "xp7deploymentsCache.yaml" );
        List<V1alpha2Xp7Deployment> deployments = this.mapper.readValue( cache, new TypeReference<>()
        {
        } );
        deployments.forEach( deploymentCache::put );

        // Add vhosts to cache
        cache = testFileSupplier.getFile( TestAdmissionApi.class, "xp7VHostCache.yaml" );
        List<V1alpha2Xp7VHost> vHosts = this.mapper.readValue( cache, new TypeReference<>()
        {
        } );
        vHosts.forEach( vHostCache::put );

        return testFileSupplier.createTests( TestAdmissionApi.class, this::runTest, "xp7deploymentsCache.yaml", "xp7VHostCache.yaml" );
    }

    @SuppressWarnings("unchecked")
    private void runTest( File file )
    {
        try
        {
            Map<String, Object> testFile = mapper.readValue( file, Map.class );
            Map<String, Object> testReview = (Map<String, Object>) testFile.get( "testReview" );
            Object testResult = testFile.get( "testResult" );
            AdmissionReview review = validate( mapper.writeValueAsString( testReview ) );
            Assertions.assertEquals( testResult.equals( "" ) ? null : testResult, review.getResponse().getStatus().getMessage() );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
}
