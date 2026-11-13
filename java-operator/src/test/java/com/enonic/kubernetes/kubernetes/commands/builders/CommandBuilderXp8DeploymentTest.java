package com.enonic.kubernetes.kubernetes.commands.builders;

import java.io.InputStream;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;

import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.crd.v1.Xp8DeploymentStatus;
import com.enonic.kubernetes.kubernetes.commands.K8sCommand;
import com.enonic.kubernetes.kubernetes.commands.K8sCommandAction;
import com.enonic.kubernetes.testutils.ConfigurationTestExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ConfigurationTestExtension.class)
class CommandBuilderXp8DeploymentTest
{
    @Test
    void equalSpecInDifferentInstanceIsNotUpdated()
    {
        Xp8Deployment existing = load();
        Xp8DeploymentStatus status = new Xp8DeploymentStatus();
        status.setState( Xp8DeploymentStatus.State.RUNNING );
        existing.setStatus( status );

        assertTrue( apply( existing, load() ).isEmpty() );
    }

    @Test
    void changedSpecIsUpdated()
    {
        Xp8Deployment rendered = load();
        rendered.getSpec().setXpVersion( "8.3.0" );

        Optional<K8sCommand> command = apply( load(), rendered );

        assertEquals( K8sCommandAction.UPDATE, command.orElseThrow().action() );
    }

    @SuppressWarnings("unchecked")
    private static Optional<K8sCommand> apply( final Xp8Deployment existing, final Xp8Deployment rendered )
    {
        MixedOperation<Xp8Deployment, KubernetesResourceList<Xp8Deployment>, Resource<Xp8Deployment>> client =
            mock( MixedOperation.class, RETURNS_DEEP_STUBS );
        when( client.inNamespace( existing.getMetadata().getNamespace() ).withName( existing.getMetadata().getName() ).get() ).
            thenReturn( existing );

        return new CommandBuilderXp8Deployment( client ).apply( new GenericBuilderParams<>( rendered, GenericBuilderAction.APPLY ) );
    }

    private static Xp8Deployment load()
    {
        try (InputStream in = CommandBuilderXp8DeploymentTest.class.getResourceAsStream(
            "/com/enonic/kubernetes/operator/templates/v1/xp8deployment/annotated.yaml" ))
        {
            return Serialization.unmarshal( in, Xp8Deployment.class );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }
}
