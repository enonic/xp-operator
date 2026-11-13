package com.enonic.kubernetes.operator.helpers;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.utils.Serialization;

import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.crd.v1.Xp8DeploymentStatus;
import com.enonic.kubernetes.templates.functions.Templator;
import com.enonic.kubernetes.templates.values.BaseValues;
import com.enonic.kubernetes.templates.values.ValueBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class HandlerTemplateUpdateTest
{
    private final RecordingHandler handler = new RecordingHandler();

    @Test
    void statusOnlyUpdateDoesNotRender()
    {
        Xp8Deployment oldResource = load();
        Xp8Deployment newResource = load();
        newResource.getMetadata().setResourceVersion( "2" );
        Xp8DeploymentStatus status = new Xp8DeploymentStatus();
        status.setState( Xp8DeploymentStatus.State.RUNNING );
        status.setMessage( "OK" );
        newResource.setStatus( status );
        assertNotSame( oldResource.getSpec(), newResource.getSpec() );

        handler.onUpdate( oldResource, newResource );

        assertEquals( 0, handler.handled.size() );
    }

    @Test
    void specUpdateRenders()
    {
        Xp8Deployment oldResource = load();
        Xp8Deployment newResource = load();
        newResource.getSpec().setXpVersion( "8.3.0" );

        handler.onUpdate( oldResource, newResource );

        assertEquals( List.of( newResource ), handler.handled );
    }

    @Test
    void labelUpdateRenders()
    {
        Xp8Deployment oldResource = load();
        Xp8Deployment newResource = load();
        newResource.getMetadata().getLabels().put( "extra", "label" );

        handler.onUpdate( oldResource, newResource );

        assertEquals( List.of( newResource ), handler.handled );
    }

    @Test
    void annotationUpdateRenders()
    {
        Xp8Deployment oldResource = load();
        Xp8Deployment newResource = load();
        newResource.getMetadata().getAnnotations().put( "extra", "annotation" );

        handler.onUpdate( oldResource, newResource );

        assertEquals( List.of( newResource ), handler.handled );
    }

    private Xp8Deployment load()
    {
        try (InputStream in = getClass().getResourceAsStream( "/com/enonic/kubernetes/operator/templates/v1/xp8deployment/annotated.yaml" ))
        {
            return Serialization.unmarshal( in, Xp8Deployment.class );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    private static class RecordingHandler
        extends HandlerTemplate<Xp8Deployment>
    {
        final List<Xp8Deployment> handled = new ArrayList<>();

        @Override
        protected void handle( final Xp8Deployment oldResource, final Xp8Deployment newResource )
        {
            handled.add( newResource );
        }

        @Override
        protected ValueBuilder<Xp8Deployment> getValueBuilder( final BaseValues baseValues )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Templator getTemplator()
        {
            throw new UnsupportedOperationException();
        }
    }
}
