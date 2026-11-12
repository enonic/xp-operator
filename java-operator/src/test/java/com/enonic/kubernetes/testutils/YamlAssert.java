package com.enonic.kubernetes.testutils;

import java.util.List;
import java.util.Objects;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;

public final class YamlAssert
{
    private static final ObjectMapper MAPPER =
        YAMLMapper.builder().configure( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true ).build();

    public static void assertYamlEquals( final String expected, final String actual, final String message )
    {
        final ObjectWriter writer = MAPPER.writerWithDefaultPrettyPrinter();
        final List<JsonNode> expectedDocuments = documents( expected );
        final List<JsonNode> actualDocuments = documents( actual );
        for ( int i = 0; i < Math.max( expectedDocuments.size(), actualDocuments.size() ); i++ )
        {
            final JsonNode expectedYaml = i < expectedDocuments.size() ? expectedDocuments.get( i ) : null;
            final JsonNode actualYaml = i < actualDocuments.size() ? actualDocuments.get( i ) : null;
            if ( !Objects.equals( expectedYaml, actualYaml ) )
            {
                assertionFailure().message( message )
                    .expected( expectedYaml == null ? null : writer.writeValueAsString( expectedYaml ) )
                    .actual( actualYaml == null ? null : writer.writeValueAsString( actualYaml ) )
                    .buildAndThrow();
            }
        }
    }

    private static List<JsonNode> documents( final String yaml )
    {
        try (MappingIterator<JsonNode> documents = MAPPER.readerFor( JsonNode.class ).readValues( yaml ))
        {
            return documents.readAll();
        }
    }
}
