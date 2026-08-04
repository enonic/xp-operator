package com.enonic.kubernetes.operator.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.zjsonpatch.JsonPatch;


import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.kubernetes.SearchersImpl;
import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.operator.api.admission.AdmissionApi;
import com.enonic.kubernetes.operator.api.admission.TestAdmissionApi;
import com.enonic.kubernetes.operator.api.mutation.MutationApi;
import com.enonic.kubernetes.operator.api.mutation.Patch;
import com.enonic.kubernetes.operator.api.mutation.TestMutationApi;
import com.enonic.kubernetes.testutils.TestFileSupplier;

class TestApi
{
    ObjectMapper mapper;

    ObjectMapper jsonMapper;

    TestInformerSearcher<Xp8Deployment> deploymentTestInformerSearcher;

    Searchers searchers;

    MutationApi mutationApi;

    AdmissionApi admissionApi;

    @SuppressWarnings("unchecked")
    TestApi()
    {
        mapper = new ObjectMapper( new YAMLFactory() );
        jsonMapper = new ObjectMapper();

        TestInformerSearcher emptyInformerSearcher = new TestInformerSearcher();
        deploymentTestInformerSearcher = new TestInformerSearcher<>();
        searchers = SearchersImpl.of( emptyInformerSearcher, emptyInformerSearcher, emptyInformerSearcher, emptyInformerSearcher,
                                      emptyInformerSearcher, deploymentTestInformerSearcher, emptyInformerSearcher );
        mutationApi = new TestMutationApi( mapper, searchers );
        admissionApi = new TestAdmissionApi( mapper, searchers );
    }

    @TestFactory
    Stream<DynamicTest> tests()
        throws IOException
    {
        TestFileSupplier testFileSupplier = new TestFileSupplier();

        // Add deployments to cache
        File cache = testFileSupplier.getFile( TestApi.class, "xp8deploymentsCache.yaml" );
        List<Xp8Deployment> deployments = this.mapper.readValue( cache, new TypeReference<>()
        {
        } );
        deployments.forEach( deploymentTestInformerSearcher::add );

        return testFileSupplier.createTests( TestApi.class, this::runTest, "xp8deploymentsCache.yaml", "xp8VHostCache.yaml" );
    }

    @SuppressWarnings("unchecked")
    private void runTest( File file )
    {
        try
        {
            TestFile test = mapper.readValue( file, TestFile.class );
            if ( test.disabled() )
            {
                return;
            }

            AdmissionReview review = mutationApi.mutate( test.admissionRequest() );

            List<Patch> patch = null;
            if ( review.getResponse().getPatch() != null )
            {
                String patchDecoded = new String( BaseEncoding.base64().decode( review.getResponse().getPatch() ) );
                patch = mapper.readValue( patchDecoded, new TypeReference<List<Patch>>()
                {
                } );
                review = applyPatch( patch, review );
            }

            review = admissionApi.validate( review );
            Assertions.assertEquals( test.assertException(), review.getResponse().getStatus().getMessage(), "Exception does not match" );
            if ( review.getResponse().getStatus().getMessage() == null )
            {
                Assertions.assertEquals( mapper.writeValueAsString( test.assertResult() ),
                                         mapper.writeValueAsString( review.getRequest().getObject() ), "Result does not match" );
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private AdmissionReview applyPatch( final List<Patch> patches, final AdmissionReview review )
    {
        JsonNode patchNode = jsonMapper.valueToTree( patches );
        JsonNode sourceNode = jsonMapper.valueToTree( review.getRequest().getObject() );
        JsonNode result = JsonPatch.apply( patchNode, sourceNode );

        review.getRequest().setObject( jsonMapper.convertValue( result, KubernetesResource.class ) );
        review.setResponse( null );

        return review;
    }
}
