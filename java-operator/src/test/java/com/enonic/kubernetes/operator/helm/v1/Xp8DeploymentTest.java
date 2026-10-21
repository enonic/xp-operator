package com.enonic.kubernetes.operator.helm.v1.xp8deployment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceAccount;

import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.helm.values.BaseValues;
import com.enonic.kubernetes.helm.values.ValueBuilder;
import com.enonic.kubernetes.helm.values.Values;
import com.enonic.kubernetes.operator.helm.HelmTest;
import com.enonic.kubernetes.operator.xp8deployment.OperatorXp8DeploymentHelm;

public class Xp8DeploymentTest
    extends HelmTest
{
    final ValueBuilder<Xp8Deployment> valueBuilder;

    public Xp8DeploymentTest()
    {
        super();
        valueBuilder = new OperatorXp8DeploymentHelm.Xp8DeploymentValueBuilder( new BaseValues( "44ddc40b-266c-4c99-b094-e758328fc6ba" ),
                                                                                () -> "password", this::createTestSa, "xp-operator", "xp-operator-hazelcast" );
    }

    private ServiceAccount createTestSa()
    {
        ServiceAccount sa = new ServiceAccount();
        ObjectMeta meta = new ObjectMeta();
        meta.setName( "cloudApi" );
        meta.setNamespace( "ec-system" );
        sa.setMetadata( meta );
        return sa;
    }

    @Override
    protected String chartToTest()
    {
        return "v1/xp8deployment";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object createValues( final ObjectMapper mapper, final File input )
        throws IOException
    {
        final Values values = valueBuilder.apply( mapper.readValue( input, Xp8Deployment.class ) );
        if ( input.getName().startsWith( "registry_prefix" ) )
        {
            // Helm is a singleton, so the registry prefix case cannot get its own test class with its own base values
            final Map<String, Object> valuesMap = (Map<String, Object>) values;
            final Map<String, Object> image = new HashMap<>( (Map<String, Object>) valuesMap.get( "image" ) );
            image.put( "registryPrefix", "europe-north1-docker.pkg.dev/my-project/dockerhub-proxy/" );
            valuesMap.put( "image", image );
        }
        return values;
    }
}
