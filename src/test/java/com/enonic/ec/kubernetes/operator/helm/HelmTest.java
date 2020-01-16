package com.enonic.ec.kubernetes.operator.helm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.enonic.ec.kubernetes.TestFileSupplier;
import com.enonic.ec.kubernetes.operator.common.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class HelmTest
    extends TestFileSupplier
{
    private Helm helm;

    protected ObjectMapper mapper;

    private ChartRepository chartRepository;

    public HelmTest()
    {
        mapper = new ObjectMapper( new YAMLFactory() );
        helm = new Helm();
        chartRepository = new LocalRepository( new File( Configuration.cfgStr( "operator.helm.charts.path" ) ) );
    }

    protected void test( String chartName, Object values, String expectedResultFile )
        throws IOException
    {
        String expectedResult = Files.readString( new File( expectedResultFile ).toPath(), StandardCharsets.UTF_8 );
        assertEquals( expectedResult, helm.template( chartRepository.get( chartName ), values ) );
    }

    protected Map<File, String> createPairs()
    {
        Map<File, String> pairs = new HashMap<>();
        List<File> files = getFiles( this.getClass(), ".yaml" );
        files.stream().
            filter( f -> !f.getAbsolutePath().endsWith( "result.yaml" ) ).
            forEach( f -> pairs.put( f, f.getAbsolutePath().replace( ".yaml", "_result.yaml" ) ) );
        return pairs;
    }

    protected abstract String chartToTest();

    protected abstract Object createValues( ObjectMapper mapper, File input )
        throws IOException;

    @TestFactory
    public Stream<DynamicTest> createTests()
    {
        String classResourcePath = getClassFilePath( this.getClass() );
        return createPairs().entrySet().stream().
            map( e -> DynamicTest.dynamicTest( testName( classResourcePath, e.getKey() ), e.getKey().toURI(), () -> {
                test( chartToTest(), createValues( mapper, e.getKey() ), e.getValue() );
            } ) );
    }
}