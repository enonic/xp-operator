package com.enonic.kubernetes.operator.templates.v1.xp8deployment;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import tools.jackson.databind.ObjectMapper;

import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.templates.values.BaseValues;
import com.enonic.kubernetes.operator.templates.TemplateTest;
import com.enonic.kubernetes.operator.xp8deployment.OperatorXp8DeploymentTemplate;
import com.enonic.kubernetes.operator.xp8deployment.Xp8DeploymentTemplator;
import com.enonic.kubernetes.templates.functions.Templator;

public class Xp8DeploymentTest
    extends TemplateTest
{
    private static final String CLUSTER_ID = "44ddc40b-266c-4c99-b094-e758328fc6ba";

    final Templator templator = new Xp8DeploymentTemplator();

    @Override
    protected Templator templator()
    {
        return templator;
    }

    @Override
    protected Object createValues( final ObjectMapper mapper, final File input )
        throws IOException
    {
        final BaseValues baseValues = new BaseValues( CLUSTER_ID, readOverrides( input ) );
        return new OperatorXp8DeploymentTemplate.Xp8DeploymentValueBuilder( baseValues, "xp-operator", "xp-operator-hazelcast" ).apply(
            mapper.readValue( input, Xp8Deployment.class ) );
    }

    private static Map<String, String> readOverrides( final File input )
        throws IOException
    {
        final Path file = input.toPath().resolveSibling( input.getName().replace( ".yaml", ".properties" ) );
        if ( !Files.exists( file ) )
        {
            return Map.of();
        }
        final Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader( file ))
        {
            properties.load( reader );
        }
        return properties.stringPropertyNames().stream().collect( Collectors.toMap( name -> name, properties::getProperty ) );
    }
}
