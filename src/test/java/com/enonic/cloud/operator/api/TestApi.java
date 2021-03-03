package com.enonic.cloud.operator.api;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.BaseEncoding;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.SearchersImpl;
import com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.cloud.operator.api.admission.AdmissionApi;
import com.enonic.cloud.operator.api.admission.TestAdmissionApi;
import com.enonic.cloud.operator.api.mutation.MutationApi;
import com.enonic.cloud.operator.api.mutation.Patch;
import com.enonic.cloud.operator.api.mutation.TestMutationApi;
import com.enonic.cloud.testutils.TestFileSupplier;

class TestApi
{
    ObjectMapper mapper;

    ObjectMapper jsonMapper;

    TestInformerSearcher<Xp7Deployment> deploymentTestInformerSearcher;

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
                                      emptyInformerSearcher, emptyInformerSearcher, deploymentTestInformerSearcher, emptyInformerSearcher,
                                      emptyInformerSearcher );
        mutationApi = new TestMutationApi( mapper, searchers );
        admissionApi = new TestAdmissionApi( mapper, searchers );
    }

    @TestFactory
    Stream<DynamicTest> tests()
        throws IOException
    {
        TestFileSupplier testFileSupplier = new TestFileSupplier();

        // Add deployments to cache
        File cache = testFileSupplier.getFile( TestApi.class, "xp7deploymentsCache.yaml" );
        List<Xp7Deployment> deployments = this.mapper.readValue( cache, new TypeReference<>()
        {
        } );
        deployments.forEach( deploymentTestInformerSearcher::add );

        return testFileSupplier.createTests( TestApi.class, this::runTest, "xp7deploymentsCache.yaml", "xp7VHostCache.yaml" );
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

            AdmissionReview review = mutationApi.validate( test.admissionRequest() );

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
        throws JsonProcessingException
    {
        JsonPatchBuilder jsonPatchBuilder = Json.createPatchBuilder();
        for ( Patch patch : patches )
        {
            // Convert value into JsonValue
            JsonReader jsonReader = Json.createReader( new StringReader( jsonMapper.writeValueAsString( patch ) ) );
            JsonObject p = jsonReader.readObject();
            jsonReader.close();
            JsonValue val = p.getValue( "/value" );

            switch ( patch.op() )
            {
                case "add":
                    jsonPatchBuilder.add( patch.path(), val );
                    break;
                case "replace":
                    jsonPatchBuilder.replace( patch.path(), val );
                    break;
                case "remove":
                    jsonPatchBuilder.remove( patch.path() );
                    break;
            }
        }

        // Apply the patch
        JsonReader reader = Json.createReader( new StringReader( jsonMapper.writeValueAsString( review.getRequest().getObject() ) ) );
        JsonStructure json = reader.read();
        json = jsonPatchBuilder.build().apply( json );
        reader.close();
        review.getRequest().setObject( jsonMapper.readValue( json.toString(), KubernetesResource.class ) );
        review.setResponse( null );

        return review;
    }
}
