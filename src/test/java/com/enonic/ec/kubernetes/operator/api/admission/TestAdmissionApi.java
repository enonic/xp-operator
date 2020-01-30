//package com.enonic.ec.kubernetes.operator.api.admission;
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
//import com.enonic.ec.kubernetes.TestFileSupplier;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource;
//import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResource;
//
//import static com.enonic.ec.kubernetes.operator.common.Configuration.cfgStr;
//
//public class TestAdmissionApi
//    extends AdmissionApi
//{
//    public TestAdmissionApi()
//    {
//        mapper = new ObjectMapper( new YAMLFactory() );
//        allNodesPicker = cfgStr( "operator.deployment.xp.allNodesKey" );
//        xp7DeploymentCache = new TestXp7DeploymentCache();
//        xp7VHostCache = new TestXp7VHostCache();
//    }
//
//    @TestFactory
//    public Stream<DynamicTest> tests()
//        throws IOException
//    {
//        TestFileSupplier testFileSupplier = new TestFileSupplier();
//
//        // Add deployments to cache
//        File cache = testFileSupplier.getFile( TestAdmissionApi.class, "xp7deploymentsCache.yaml" );
//        List<Xp7DeploymentResource> deployments = this.mapper.readValue( cache, new TypeReference<List<Xp7DeploymentResource>>()
//        {
//        } );
//        deployments.forEach( d -> ( (TestXp7DeploymentCache) xp7DeploymentCache ).put( d ) );
//
//        // Add vhosts to cache
//        cache = testFileSupplier.getFile( TestAdmissionApi.class, "xp7VHostCache.yaml" );
//        List<Xp7VHostResource> vHosts = this.mapper.readValue( cache, new TypeReference<List<Xp7VHostResource>>()
//        {
//        } );
//        vHosts.forEach( d -> ( (TestXp7VHostCache) xp7VHostCache ).put( d ) );
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
