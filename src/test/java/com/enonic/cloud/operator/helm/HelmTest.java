package com.enonic.cloud.operator.helm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.operator.common.Configuration;
import com.enonic.cloud.testutils.TestFileSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class HelmTest
    extends TestFileSupplier
{
    protected ObjectMapper mapper;

    private Helm helm;

    private ChartRepository chartRepository;

    public HelmTest()
    {
        mapper = new ObjectMapper( new YAMLFactory() );
        helm = new Helm();
        chartRepository = new LocalRepository( new File( Configuration.cfgStr( "operator.helm.charts.path" ) ) );
    }

    protected void test( String chartName, Object values, String expectedValuesFile, String expectedResultFile )
        throws IOException
    {
        String expectedValues = Files.readString( new File( expectedValuesFile ).toPath(), StandardCharsets.UTF_8 );

        assertEquals( expectedValues, mapper.writeValueAsString( values ),
                      "Values do not match (" + new File( expectedValuesFile ).getName() + ":0)" );

        String expectedResult = Files.readString( new File( expectedResultFile ).toPath(), StandardCharsets.UTF_8 );
        StringBuilder sb = new StringBuilder();
        for ( HasMetadata r : helm.templateObjects( chartRepository.get( chartName ), values ) )
        {
            sb.append( mapper.writeValueAsString( r ) );
        }
        assertEquals( expectedResult, sb.toString(), "Result does not match: (" + new File( expectedResultFile ).getName() + ":0)" );
    }

    protected abstract String chartToTest();

    protected abstract Object createValues( ObjectMapper mapper, File input )
        throws IOException;

    @TestFactory
    public Stream<DynamicTest> createTests()
    {
        String classResourcePath = getClassFilePath( this.getClass() );
        return getFiles( this.getClass(), ".yaml" ).stream().
            filter( f -> !f.getAbsolutePath().endsWith( "result.yaml" ) ).
            filter( f -> !f.getAbsolutePath().endsWith( "values.yaml" ) ).
            map( f -> DynamicTest.dynamicTest( testName( classResourcePath, f ), f.toURI(), () -> {
                test( chartToTest(), createValues( mapper, f ), f.getAbsolutePath().replace( ".yaml", "_values.yaml" ),
                      f.getAbsolutePath().replace( ".yaml", "_result.yaml" ) );
            } ) );
    }
}