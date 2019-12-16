package com.enonic.ec.kubernetes.operator.api.admission;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import io.fabric8.kubernetes.api.model.admission.AdmissionReview;

import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;

public abstract class AdmissionApiTest
    extends AdmissionApi
{
    public AdmissionApiTest()
    {
        mapper = new ObjectMapper( new YAMLFactory() );
        xp7DeploymentKind = ConfigProvider.getConfig().getValue( "operator.crd.xp.deployments.kind", String.class );
        xp7vHostKind = ConfigProvider.getConfig().getValue( "operator.crd.xp.vhosts.kind", String.class );
        xp7ConfigKind = ConfigProvider.getConfig().getValue( "operator.crd.xp.configs.kind", String.class );
        xp7AppKind = ConfigProvider.getConfig().getValue( "operator.crd.xp.apps.kind", String.class );
        allNodesPicker = ConfigProvider.getConfig().getValue( "operator.deployment.xp.allNodes", String.class );
    }

    @BeforeEach
    protected void setup()
    {
        this.xpDeploymentCache = new TestDeploymentCache();
    }

    @AfterEach
    protected void teardown()
    {
        this.xpDeploymentCache = null;
    }

    private String getFileContents( String file )
    {
        try
        {
            return Resources.toString( AdmissionApiTest.class.getResource( file ), Charsets.UTF_8 );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    protected XpDeploymentCache getDeploymentsCache( String deploymentsFile )
    {
        try
        {
            List<XpDeploymentResource> deployments =
                this.mapper.readValue( getFileContents( deploymentsFile ), new TypeReference<List<XpDeploymentResource>>()
                {
                } );
            TestDeploymentCache cache = new TestDeploymentCache();
            deployments.forEach( d -> cache.put( d ) );
            return cache;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    protected void assertAdmissionReview( String file, String message )
    {
        AdmissionReview review = null;
        try
        {
            review = validate( getFileContents( file ) );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        Assertions.assertEquals( message, review.getResponse().getStatus().getReason() );
    }

    protected Stream<DynamicTest> createStream( Map<String, String> testMap, Runnable init )
    {
        return testMap.entrySet().stream().
            map( e -> {
                try
                {
                    return DynamicTest.dynamicTest( e.getKey(), AdmissionApiTest.class.getResource( e.getKey() ).toURI(), () -> {
                        init.run();
                        assertAdmissionReview( e.getKey(), e.getValue() );
                    } );
                }
                catch ( URISyntaxException u )
                {
                    throw new RuntimeException( u );
                }
            } );
    }


    protected Stream<DynamicTest> createStreamWithDeploymentCache( String deploymentsFile, Consumer<Map<String, String>> mapCreator )
    {
        XpDeploymentCache cache = getDeploymentsCache( deploymentsFile );
        return createStream( mapCreator, () -> this.xpDeploymentCache = cache );
    }

    protected Stream<DynamicTest> createStream( Consumer<Map<String, String>> mapCreator )
    {
        return createStream( mapCreator, () -> {
        } );
    }

    protected Stream<DynamicTest> createStream( Consumer<Map<String, String>> mapCreator, Runnable init )
    {
        Map<String, String> tests = new HashMap<>();
        mapCreator.accept( tests );
        return createStream( tests, init );
    }
}