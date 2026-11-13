package com.enonic.kubernetes.operator.xp8config;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.ReplaceDeletable;
import io.fabric8.kubernetes.client.dsl.Resource;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.crd.v1.Xp8ConfigStatus;
import com.enonic.kubernetes.kubernetes.Clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperatorXp8ConfigStatusTest
{
    private final OperatorXp8ConfigStatus operator = new OperatorXp8ConfigStatus();

    private final ReplaceDeletable<Xp8Config> replaceable = mock();

    private Xp8Config stored;

    @BeforeEach
    void setUp()
    {
        final MixedOperation<Xp8Config, KubernetesResourceList<Xp8Config>, Resource<Xp8Config>> configs = mock();
        final NonNamespaceOperation<Xp8Config, KubernetesResourceList<Xp8Config>, Resource<Xp8Config>> inNamespace = mock();
        final Resource<Xp8Config> resource = mock();
        when( configs.inNamespace( "ns" ) ).thenReturn( inNamespace );
        when( inNamespace.withName( "cfg" ) ).thenReturn( resource );
        when( resource.get() ).thenAnswer( i -> stored );
        when( resource.lockResourceVersion( any() ) ).thenReturn( replaceable );
        when( replaceable.replace( any() ) ).thenAnswer( i -> i.getArgument( 0 ) );
        operator.clients = new Clients( mock(), configs, mock() );
    }

    @Test
    void markReadyWithoutStatus()
    {
        stored = OperatorXp8ConfigUpdateTest.config( "a=1" );

        operator.markReady( stored );

        assertThat( replacedStatus().getState() ).isEqualTo( Xp8ConfigStatus.State.READY );
    }

    @Test
    void markPendingWithoutStatus()
    {
        stored = OperatorXp8ConfigUpdateTest.config( "a=1" );

        operator.markPending( stored, List.of( pod( "main-0" ) ) );

        assertThat( replacedStatus().getState() ).isEqualTo( Xp8ConfigStatus.State.PENDING );
        assertThat( replacedStatus().getMessage() ).isEqualTo( "Not loaded: main-0" );
    }

    @Test
    void markReadyWhenReady()
    {
        stored = OperatorXp8ConfigUpdateTest.config( "a=1" );
        final Xp8ConfigStatus status = new Xp8ConfigStatus();
        status.setState( Xp8ConfigStatus.State.READY );
        stored.setStatus( status );

        operator.markReady( stored );

        verify( replaceable, never() ).replace( any() );
    }

    private Xp8ConfigStatus replacedStatus()
    {
        final ArgumentCaptor<Xp8Config> replaced = ArgumentCaptor.forClass( Xp8Config.class );
        verify( replaceable ).replace( replaced.capture() );
        return replaced.getValue().getStatus();
    }

    private static Pod pod( final String name )
    {
        return new PodBuilder().withNewMetadata().withName( name ).endMetadata().build();
    }
}
