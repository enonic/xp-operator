package com.enonic.kubernetes.operator.templates.v1.xp8deployment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;

import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.templates.values.BaseValues;
import com.enonic.kubernetes.templates.values.ValueBuilder;
import com.enonic.kubernetes.templates.values.Values;
import com.enonic.kubernetes.operator.templates.TemplateTest;
import com.enonic.kubernetes.operator.xp8deployment.OperatorXp8DeploymentTemplate;
import com.enonic.kubernetes.operator.xp8deployment.Xp8DeploymentTemplator;
import com.enonic.kubernetes.templates.functions.Templator;

public class Xp8DeploymentTest
    extends TemplateTest
{
    final ValueBuilder<Xp8Deployment> valueBuilder;

    final Templator templator = new Xp8DeploymentTemplator();

    public Xp8DeploymentTest()
    {
        super();
        valueBuilder = new OperatorXp8DeploymentTemplate.Xp8DeploymentValueBuilder( new BaseValues( "44ddc40b-266c-4c99-b094-e758328fc6ba" ),
                                                                                "xp-operator", "xp-operator-hazelcast" );
    }

    @Override
    protected Templator templator()
    {
        return templator;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object createValues( final ObjectMapper mapper, final File input )
        throws IOException
    {
        final Values values = valueBuilder.apply( mapper.readValue( input, Xp8Deployment.class ) );
        if ( input.getName().startsWith( "registry_prefix" ) )
        {
            final Map<String, Object> valuesMap = (Map<String, Object>) values;
            final Map<String, Object> image = new HashMap<>( (Map<String, Object>) valuesMap.get( "image" ) );
            image.put( "registryPrefix", "europe-north1-docker.pkg.dev/my-project/dockerhub-proxy/" );
            valuesMap.put( "image", image );
        }
        return values;
    }
}
