package com.enonic.kubernetes.kubernetes.commands.builders;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.crd.v1.Xp8ConfigSpec;
import com.enonic.kubernetes.crd.v1.Xp8ConfigStatus;
import com.enonic.kubernetes.kubernetes.commands.K8sCommand;
import com.enonic.kubernetes.kubernetes.commands.K8sCommandAction;
import com.enonic.kubernetes.testutils.ConfigurationTestExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ConfigurationTestExtension.class)
class CommandBuilderXp8ConfigTest
{
    @Test
    void equalSpecInDifferentInstanceIsNotUpdated()
    {
        Xp8Config existing = config( "a=1" );
        Xp8ConfigStatus status = new Xp8ConfigStatus();
        status.setState( Xp8ConfigStatus.State.READY );
        existing.setStatus( status );

        assertTrue( apply( existing, config( "a=1" ) ).isEmpty() );
    }

    @Test
    void changedSpecIsUpdated()
    {
        Optional<K8sCommand> command = apply( config( "a=1" ), config( "a=2" ) );

        assertEquals( K8sCommandAction.UPDATE, command.orElseThrow().action() );
    }

    @SuppressWarnings("unchecked")
    private static Optional<K8sCommand> apply( final Xp8Config existing, final Xp8Config rendered )
    {
        MixedOperation<Xp8Config, KubernetesResourceList<Xp8Config>, Resource<Xp8Config>> client =
            mock( MixedOperation.class, RETURNS_DEEP_STUBS );
        when( client.inNamespace( "ns" ).withName( "cfg" ).get() ).thenReturn( existing );

        return new CommandBuilderXp8Config( client ).apply( new GenericBuilderParams<>( rendered, GenericBuilderAction.APPLY ) );
    }

    private static Xp8Config config( final String data )
    {
        Xp8Config config = new Xp8Config();
        config.setMetadata( new ObjectMetaBuilder().withName( "cfg" ).withNamespace( "ns" ).build() );
        Xp8ConfigSpec spec = new Xp8ConfigSpec();
        spec.setNodeGroup( "all" );
        spec.setFile( "app.cfg" );
        spec.setData( data );
        spec.setDataBase64( false );
        config.setSpec( spec );
        return config;
    }
}
