package com.enonic.kubernetes.operator.xp8config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.crd.v1.Xp8ConfigSpec;
import com.enonic.kubernetes.crd.v1.Xp8ConfigStatus;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OperatorXp8ConfigUpdateTest
{
    private final OperatorXp8Config operator = new OperatorXp8Config();

    @BeforeEach
    void setUp()
    {
        operator.operatorConfigMapSync = mock( OperatorConfigMapSync.class );
    }

    @Test
    void statusOnlyUpdateDoesNotSync()
    {
        Xp8Config oldResource = config( "a=1" );
        Xp8Config newResource = config( "a=1" );
        newResource.getMetadata().setResourceVersion( "2" );
        Xp8ConfigStatus status = new Xp8ConfigStatus();
        status.setState( Xp8ConfigStatus.State.READY );
        status.setMessage( "OK" );
        newResource.setStatus( status );

        operator.onUpdate( oldResource, newResource );

        verify( operator.operatorConfigMapSync, never() ).handle( "ns" );
    }

    @Test
    void specUpdateSyncs()
    {
        operator.onUpdate( config( "a=1" ), config( "a=2" ) );

        verify( operator.operatorConfigMapSync ).handle( "ns" );
    }

    static Xp8Config config( final String data )
    {
        Xp8Config config = new Xp8Config();
        config.setMetadata( new ObjectMetaBuilder().withName( "cfg" ).withNamespace( "ns" ).withResourceVersion( "1" ).build() );
        Xp8ConfigSpec spec = new Xp8ConfigSpec();
        spec.setNodeGroup( "all" );
        spec.setFile( "app.cfg" );
        spec.setData( data );
        spec.setDataBase64( false );
        config.setSpec( spec );
        return config;
    }
}
