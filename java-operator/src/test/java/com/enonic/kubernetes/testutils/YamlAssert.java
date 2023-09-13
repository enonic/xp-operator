package com.enonic.kubernetes.testutils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;

public final class YamlAssert
{
    private static final ObjectMapper MAPPER = new ObjectMapper( new YAMLFactory() );

    static
    {
        MAPPER.configure( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true );
    }

    public static void assertYamlEquals( final String expected, final String actual, final String message )
    {
        final ObjectReader reader = MAPPER.reader();
        final ObjectWriter writer = MAPPER.writerWithDefaultPrettyPrinter();
        try (var expectedParser = reader.createParser( expected ); var actualParser = reader.createParser( actual ))
        {
            JsonNode expectedYaml;
            JsonNode actualYaml;
            do
            {
                expectedYaml = expectedParser.readValueAsTree();
                actualYaml = actualParser.readValueAsTree();

                if ( !Objects.equals( expectedYaml, actualYaml ) )
                {
                    assertionFailure().message( message )
                        .expected( expectedYaml == null ? null : writer.writeValueAsString( expectedYaml ) )
                        .actual( actualYaml == null ? null : writer.writeValueAsString( actualYaml ) )
                        .buildAndThrow();
                }

            }
            while ( expectedYaml != null || actualYaml != null );

        }
        catch ( final IOException e )
        {
            throw new UncheckedIOException( e );
        }

    }
}
