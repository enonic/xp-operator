package com.enonic.kubernetes.operator.helm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.helm.Helm;
import com.enonic.kubernetes.helm.charts.ChartRepository;
import com.enonic.kubernetes.helm.charts.LocalRepository;
import com.enonic.kubernetes.testutils.TestFileSupplier;

import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.testutils.YamlAssert.assertYamlEquals;

public abstract class HelmTest
    extends TestFileSupplier
{
    private final ObjectMapper mapper;

    private final Helm helm;

    private final ChartRepository chartRepository;

    protected HelmTest()
    {
        mapper = new ObjectMapper( new YAMLFactory() );
        mapper.configure( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true );
        helm = new Helm();
        chartRepository = new LocalRepository( new File( cfgStr( "operator.charts.path" ) ) );
    }

    void test( String chartName, Object values, String expectedValuesFile, String expectedResultFile )
        throws IOException
    {
        assertYamlEquals(Files.readString( Path.of( expectedValuesFile ) ), mapper.writeValueAsString(values),
                         "Values do not match (" + Path.of(expectedValuesFile).getFileName() + ":0)");

        StringBuilder sb = new StringBuilder();
        for ( HasMetadata r : helm.templateObjects( chartRepository.get( chartName ), values ) )
        {
            sb.append( mapper.writeValueAsString( r ) );
        }

        assertYamlEquals( Files.readString( Path.of( expectedResultFile ) ), sb.toString(),
                          "Result does not match: (" + Path.of( expectedResultFile ).getFileName() + ":0)" );
    }

    @SuppressWarnings("SameReturnValue")
    protected abstract String chartToTest();

    protected abstract Object createValues( ObjectMapper mapper, File input )
        throws IOException;

    @TestFactory
    Stream<DynamicTest> createTests()
    {
        String classResourcePath = getClassFilePath( this.getClass() );
        return getFiles( this.getClass() ).stream().
            filter( f -> !f.getAbsolutePath().endsWith( "_result.yaml" ) ).
            filter( f -> !f.getAbsolutePath().endsWith( "_values.yaml" ) ).
            map( f -> DynamicTest.dynamicTest( testName( classResourcePath, f ), f.toURI(),
                                               () -> test( chartToTest(), createValues( mapper, f ),
                                                           f.getAbsolutePath().replace( ".yaml", "_values.yaml" ),
                                                           f.getAbsolutePath().replace( ".yaml", "_result.yaml" ) ) ) );
    }
}
