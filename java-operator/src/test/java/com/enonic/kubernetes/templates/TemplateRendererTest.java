package com.enonic.kubernetes.templates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateRendererTest
{
    private final TemplateRenderer renderer = new TemplateRenderer( "templates/test", Map.of() );

    @Test
    void rendersAnObjectPerDocument()
    {
        final List<HasMetadata> rendered = renderer.render( "configmap.yaml", model( "my-name" ) );

        assertThat( rendered ).singleElement().isInstanceOf( ConfigMap.class );
        assertThat( rendered.get( 0 ).getMetadata().getName() ).isEqualTo( "my-name" );
    }

    @Test
    void aValueCannotChangeTheStructureOfTheDocument()
    {
        final String name = "x\n  namespace: injected";

        final HasMetadata rendered = renderer.render( "configmap.yaml", model( name ) ).get( 0 );

        assertThat( rendered.getMetadata().getName() ).isEqualTo( name );
        assertThat( rendered.getMetadata().getNamespace() ).isNull();
    }

    @Test
    void aMissingValueLeavesTheFieldUnset()
    {
        final ConfigMap rendered = (ConfigMap) renderer.render( "configmap.yaml", model( "my-name" ) ).get( 0 );

        assertThat( rendered.getData() ).containsEntry( "missing", null );
    }

    @Test
    void anEmptyValueStaysAnEmptyString()
    {
        final Map<String, Object> model = model( "my-name" );
        model.put( "annotations", Map.of( "flag", "" ) );

        final HasMetadata rendered = renderer.render( "configmap.yaml", model ).get( 0 );

        assertThat( rendered.getMetadata().getAnnotations() ).containsExactly( Map.entry( "flag", "" ) );
    }

    @Test
    void aMapIsWrittenAsItIs()
    {
        final HasMetadata rendered = renderer.render( "configmap.yaml", model( "my-name" ) ).get( 0 );

        assertThat( rendered.getMetadata().getLabels() ).containsExactly( Map.entry( "app", "a: b" ) );
    }

    @Test
    void entriesLoopsOverAMap()
    {
        final HasMetadata rendered = renderer.render( "configmap.yaml", model( "my-name" ) ).get( 0 );

        assertThat( rendered.getMetadata().getAnnotations() ).containsExactly( Map.entry( "note", "true" ) );
    }

    @Test
    void rawWritesAValueInsideAString()
    {
        final ConfigMap rendered = (ConfigMap) renderer.render( "configmap.yaml", model( "my-name" ) ).get( 0 );

        assertThat( rendered.getData() ).containsEntry( "greeting", "hello my-name" );
    }

    @Test
    void fileWritesTheContentOfAFileAsAString()
    {
        final ConfigMap rendered = (ConfigMap) renderer.render( "configmap.yaml", model( "my-name" ) ).get( 0 );

        assertThat( rendered.getData() ).containsEntry( "included", "first line\nsecond: line\n" );
    }

    @Test
    void aTemplateCannotCallAMethod()
    {
        assertThatThrownBy( () -> renderer.render( "method_call.yaml", model( "my-name" ) ) )
            .hasStackTraceContaining( "may only read maps and lists" );
    }

    @Test
    void aTemplateCannotLoopOverAMapWithoutEntries()
    {
        assertThatThrownBy( () -> renderer.render( "map_entries.yaml", model( "my-name" ) ) )
            .hasStackTraceContaining( "may only read maps and lists" );
    }

    private static Map<String, Object> model( final String name )
    {
        final Map<String, Object> model = new HashMap<>();
        model.put( "name", name );
        model.put( "labels", Map.of( "app", "a: b" ) );
        model.put( "annotations", Map.of( "note", true ) );
        return model;
    }
}
