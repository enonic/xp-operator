package com.enonic.kubernetes.operator.xp8deployment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import com.enonic.kubernetes.templates.TemplateRenderer;
import com.enonic.kubernetes.templates.functions.Templator;
import com.enonic.kubernetes.templates.values.Values;

@NullMarked
@Singleton
@Named("v1/xp8deployment")
public class Xp8DeploymentTemplator
    implements Templator
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TemplateRenderer renderer =
        new TemplateRenderer( "templates/v1/xp8deployment", Map.of( "imageRef", new ImageRefFunction() ) );

    @Override
    public List<HasMetadata> apply( final Values values )
    {
        return renderer.render( "xp8deployment.yaml", MAPPER.convertValue( values, new TypeReference<LinkedHashMap<String, @Nullable Object>>()
        {
        } ) );
    }

    private static final class ImageRefFunction
        implements Function
    {
        @Override
        public Object execute( final Map<String, Object> args, final PebbleTemplate self, final EvaluationContext context,
                               final int lineNumber )
        {
            return ImageRef.resolve( String.valueOf( args.get( "image" ) ), Objects.toString( args.get( "prefix" ), "" ) );
        }

        @Override
        public List<String> getArgumentNames()
        {
            return List.of( "image", "prefix" );
        }
    }
}
