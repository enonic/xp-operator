package com.enonic.kubernetes.templates;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.extension.escaper.SafeString;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

@NullMarked
public class TemplateRenderer
{
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final ObjectMapper YAML = new ObjectMapper( new YAMLFactory() );

    private final String directory;

    private final PebbleEngine engine;

    public TemplateRenderer( final String directory, final Map<String, Function> functions )
    {
        this.directory = directory;

        final ClasspathLoader loader = new ClasspathLoader( TemplateRenderer.class.getClassLoader() );
        loader.setPrefix( directory );

        final Map<String, Function> allFunctions = new HashMap<>( functions );
        allFunctions.put( "file", new FileFunction() );

        this.engine = new PebbleEngine.Builder()
            .loader( loader )
            .newLineTrimming( false )
            .methodAccessValidator( ( object, method ) -> {
                throw new IllegalStateException(
                    "Template called " + method.getDeclaringClass().getName() + "." + method.getName() + "(), but may only read maps and lists" );
            } )
            .addEscapingStrategy( "yaml", TemplateRenderer::json )
            .defaultEscapingStrategy( "yaml" )
            .extension( new AbstractExtension()
            {
                @Override
                public Map<String, Filter> getFilters()
                {
                    return Map.of( "toJson", new ToJsonFilter(), "entries", new EntriesFilter() );
                }

                @Override
                public Map<String, Function> getFunctions()
                {
                    return allFunctions;
                }
            } )
            .build();
    }

    public List<HasMetadata> render( final String name, final Map<String, @Nullable Object> model )
    {
        try
        {
            final StringWriter yaml = new StringWriter();
            engine.getTemplate( name ).evaluate( yaml, model );

            final List<HasMetadata> res = new ArrayList<>();
            try (MappingIterator<JsonNode> documents = YAML.readerFor( JsonNode.class ).readValues( yaml.toString() ))
            {
                while ( documents.hasNext() )
                {
                    final JsonNode document = documents.next();
                    if ( document != null && document.isObject() )
                    {
                        res.add( YAML.treeToValue( document, HasMetadata.class ) );
                    }
                }
            }
            return res;
        }
        catch ( IOException | RuntimeException e )
        {
            throw new IllegalStateException( "Template '" + directory + "/" + name + "' did not render to Kubernetes objects", e );
        }
    }

    private String read( final String name )
    {
        try (InputStream in = TemplateRenderer.class.getClassLoader().getResourceAsStream( directory + "/" + name ))
        {
            if ( in == null )
            {
                throw new IllegalArgumentException( "File '" + directory + "/" + name + "' not found" );
            }
            return new String( in.readAllBytes(), StandardCharsets.UTF_8 );
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }

    private static String json( final @Nullable Object value )
    {
        try
        {
            return JSON.writeValueAsString( value );
        }
        catch ( JacksonException e )
        {
            throw new IllegalStateException( e );
        }
    }

    private static final class ToJsonFilter
        implements Filter
    {
        @Override
        public Object apply( final @Nullable Object input, final Map<String, Object> args, final PebbleTemplate self,
                             final EvaluationContext context, final int lineNumber )
        {
            return new SafeString( input == null ? "" : json( input ) );
        }

        @Override
        public List<String> getArgumentNames()
        {
            return List.of();
        }
    }

    private static final class EntriesFilter
        implements Filter
    {
        @Override
        public Object apply( final @Nullable Object input, final Map<String, Object> args, final PebbleTemplate self,
                             final EvaluationContext context, final int lineNumber )
        {
            final List<Map<String, @Nullable Object>> res = new ArrayList<>();
            if ( input instanceof Map<?, ?> map )
            {
                map.forEach( ( key, value ) -> {
                    final Map<String, @Nullable Object> entry = new HashMap<>();
                    entry.put( "key", key );
                    entry.put( "value", value );
                    res.add( entry );
                } );
            }
            return res;
        }

        @Override
        public List<String> getArgumentNames()
        {
            return List.of();
        }
    }

    private final class FileFunction
        implements Function
    {
        @Override
        public Object execute( final Map<String, Object> args, final PebbleTemplate self, final EvaluationContext context,
                               final int lineNumber )
        {
            return new SafeString( json( read( String.valueOf( args.get( "name" ) ) ) ) );
        }

        @Override
        public List<String> getArgumentNames()
        {
            return List.of( "name" );
        }
    }
}
