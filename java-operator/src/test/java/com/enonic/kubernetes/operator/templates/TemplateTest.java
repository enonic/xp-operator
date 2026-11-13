package com.enonic.kubernetes.operator.templates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.templates.functions.Templator;
import com.enonic.kubernetes.templates.values.Values;
import com.enonic.kubernetes.testutils.TestFileSupplier;

import static com.enonic.kubernetes.testutils.YamlAssert.assertYamlEquals;

public abstract class TemplateTest
    extends TestFileSupplier
{
    private final ObjectMapper mapper;

    protected TemplateTest()
    {
        mapper = new ObjectMapper( new YAMLFactory() );
        mapper.configure( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true );
        mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
    }

    void test( Object values, String expectedValuesFile, String expectedResultFile )
        throws IOException
    {
        assertYamlEquals(Files.readString( Path.of( expectedValuesFile ) ), mapper.writeValueAsString(values),
                         "Values do not match (" + Path.of(expectedValuesFile).getFileName() + ":0)");

        final String expectedResult = Files.readString( Path.of( expectedResultFile ) );

        final List<String> expectedOrder = new ArrayList<>();
        try (MappingIterator<JsonNode> documents = mapper.readerFor( JsonNode.class ).readValues( expectedResult ))
        {
            documents.forEachRemaining( d -> expectedOrder.add( d.path( "kind" ).asText() + "#" + d.path( "metadata" ).path( "name" ).asText() ) );
        }
        final List<HasMetadata> rendered = new ArrayList<>( templator().apply( (Values) values ) );
        rendered.sort( Comparator.comparingInt( r -> {
            final int index = expectedOrder.indexOf( r.getKind() + "#" + r.getMetadata().getName() );
            return index < 0 ? Integer.MAX_VALUE : index;
        } ) );

        StringBuilder sb = new StringBuilder();
        for ( HasMetadata r : rendered )
        {
            sb.append( mapper.writeValueAsString( r ) );
        }

        assertYamlEquals( expectedResult, sb.toString(),
                          "Result does not match: (" + Path.of( expectedResultFile ).getFileName() + ":0)" );
    }

    protected abstract Templator templator();

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
                                               () -> test( createValues( mapper, f ),
                                                           f.getAbsolutePath().replace( ".yaml", "_values.yaml" ),
                                                           f.getAbsolutePath().replace( ".yaml", "_result.yaml" ) ) ) );
    }
}
