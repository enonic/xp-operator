//package com.enonic.cloud.operator.api.admission;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Stream;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.DynamicTest;
//import org.junit.jupiter.api.TestFactory;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
//
//import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
//
//import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
//import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
//import com.enonic.cloud.testutils.TestFileSupplier;
//
//import static com.enonic.cloud.common.Configuration.cfgStr;
//
//class TestAdmissionApi
//    extends AdmissionApi
//{
//    TestAdmissionApi()
//    {
//        mapper = new ObjectMapper( new YAMLFactory() );
//        allNodesPicker = cfgStr( "operator.helm.charts.Values.allNodesKey" );
//        v1alpha2Xp7DeploymentCache = new TestXp7DeploymentCache();
//        v1alpha2Xp7VHostCache = new TestXp7VHostCache();
//    }
//
//    @TestFactory
//    Stream<DynamicTest> tests()
//        throws IOException
//    {
//        TestFileSupplier testFileSupplier = new TestFileSupplier();
//
//        // Add deployments to cache
//        File cache = testFileSupplier.getFile( TestAdmissionApi.class, "xp7deploymentsCache.yaml" );
//        List<V1alpha2Xp7Deployment> deployments = this.mapper.readValue( cache, new TypeReference<>()
//        {
//        } );
//        deployments.forEach( d -> ( (TestXp7DeploymentCache) v1alpha2Xp7DeploymentCache ).put( d ) );
//
//        // Add vhosts to cache
//        cache = testFileSupplier.getFile( TestAdmissionApi.class, "xp7VHostCache.yaml" );
//        List<V1alpha2Xp7VHost> vHosts = this.mapper.readValue( cache, new TypeReference<>()
//        {
//        } );
//        vHosts.forEach( v -> ( (TestXp7VHostCache) v1alpha2Xp7VHostCache ).put( v ) );
//
//        return testFileSupplier.createTests( TestAdmissionApi.class, this::runTest, "xp7deploymentsCache.yaml", "xp7VHostCache.yaml" );
//    }
//
//    @SuppressWarnings("unchecked")
//    private void runTest( File file )
//    {
//        try
//        {
//            Map<String, Object> testFile = mapper.readValue( file, Map.class );
//            Map<String, Object> testReview = (Map<String, Object>) testFile.get( "testReview" );
//            Object testResult = testFile.get( "testResult" );
//            AdmissionReview review = validate( mapper.writeValueAsString( testReview ) );
//            Assertions.assertEquals( testResult.equals( "" ) ? null : testResult, review.getResponse().getStatus().getMessage() );
//        }
//        catch ( IOException e )
//        {
//            throw new RuntimeException( e );
//        }
//    }
//}
