package com.enonic.kubernetes.operator.xp8config;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
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
import com.enonic.kubernetes.kubernetes.Searchers;
import com.enonic.kubernetes.operator.api.TestInformerSearcher;

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

    private final TestInformerSearcher<ConfigMap> configMaps = new TestInformerSearcher<>();

    private final TestInformerSearcher<Pod> pods = new TestInformerSearcher<>();

    private final TestInformerSearcher<Xp8Config> xp8Configs = new TestInformerSearcher<>();

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
        operator.configMapUpdatedAnnotation = "updated";
        operator.podConfigReloadedAnnotation = "reloaded";
        operator.nodeGroupLabel = "group";
        operator.allNodesKey = "all";
        operator.searchers = new Searchers( configMaps, new TestInformerSearcher<>(), new TestInformerSearcher<>(), pods, xp8Configs,
                                            new TestInformerSearcher<>(), new TestInformerSearcher<>() );
    }

    @Test
    void podEventMarksPendingOnceIgnoringUnmanagedConfigMaps()
    {
        stored = config( "all" );
        xp8Configs.add( stored );
        configMaps.add( new ConfigMapBuilder().withNewMetadata().withName( "kube-root-ca.crt" ).withNamespace( "ns" ).endMetadata().build() );
        configMaps.add( managed( new ConfigMapBuilder().withNewMetadata()
                                     .withName( "extra-config" )
                                     .addToAnnotations( "updated", "2026-11-20T10:30:00Z" )
                                     .endMetadata()
                                     .build() ) );
        configMaps.add( managed( configMap( "main", "10:00" ) ) );
        final Pod pod = managed( pod( "main-0", "main", "09:59" ) );
        pods.add( pod );

        operator.onNewAdd( pod );

        assertThat( replacedStatus().getState() ).isEqualTo( Xp8ConfigStatus.State.PENDING );
        assertThat( replacedStatus().getMessage() ).isEqualTo( "Not loaded: main-0" );
    }

    @Test
    void podEventMarksReadyOnceThePodReloaded()
    {
        stored = config( "all" );
        final Xp8ConfigStatus pending = new Xp8ConfigStatus();
        pending.setState( Xp8ConfigStatus.State.PENDING );
        stored.setStatus( pending );
        xp8Configs.add( stored );
        configMaps.add( managed( configMap( "main", "10:00" ) ) );
        final Pod reloaded = managed( pod( "main-0", "main", "10:01" ) );
        pods.add( reloaded );

        operator.onUpdate( managed( pod( "main-0", "main", "09:59" ) ), reloaded );

        assertThat( replacedStatus().getState() ).isEqualTo( Xp8ConfigStatus.State.READY );
    }

    @Test
    void unmanagedPodsAndUnchangedPodsAreIgnored()
    {
        stored = config( "all" );
        xp8Configs.add( stored );
        configMaps.add( managed( configMap( "main", "10:00" ) ) );
        final Pod managedPod = managed( pod( "main-0", "main", "09:59" ) );
        pods.add( managedPod );

        operator.onNewAdd( pod( "other-0", "main", "09:59" ) );
        operator.onUpdate( managedPod, managedPod );
        operator.onDelete( managedPod, false );

        verify( replaceable, never() ).replace( any() );
    }

    @Test
    void notLoadedUntilThePodReloadsAfterTheUpdate()
    {
        final Xp8Config config = config( "all" );
        final List<ConfigMap> configMaps = List.of( configMap( "main", "10:00" ) );

        assertThat( operator.notLoaded( config, configMaps, List.of( pod( "main-0", "main", "09:59" ) ) ) ).hasValueSatisfying(
            pods -> assertThat( pods ).extracting( p -> p.getMetadata().getName() ).containsExactly( "main-0" ) );
        assertThat( operator.notLoaded( config, configMaps, List.of( pod( "main-0", "main", "10:01" ) ) ) ).hasValueSatisfying(
            pods -> assertThat( pods ).isEmpty() );
    }

    @Test
    void allNodesConfigComparesEachPodWithItsOwnNodeGroup()
    {
        final List<ConfigMap> configMaps = List.of( configMap( "master", "10:05" ), configMap( "data", "10:00" ) );
        final List<Pod> pods = List.of( pod( "master-0", "master", "10:06" ), pod( "data-0", "data", "10:01" ) );

        assertThat( operator.notLoaded( config( "all" ), configMaps, pods ) ).hasValueSatisfying( p -> assertThat( p ).isEmpty() );
    }

    @Test
    void nodeGroupConfigIgnoresOtherNodeGroups()
    {
        final List<ConfigMap> configMaps = List.of( configMap( "master", "10:05" ), configMap( "data", "10:00" ) );
        final List<Pod> pods = List.of( pod( "master-0", "master", "10:04" ), pod( "data-0", "data", "10:01" ) );

        assertThat( operator.notLoaded( config( "data" ), configMaps, pods ) ).hasValueSatisfying( p -> assertThat( p ).isEmpty() );
    }

    @Test
    void untrackedWithoutAnUpdatedNodeGroupConfigMap()
    {
        final ConfigMap neverUpdated = new ConfigMapBuilder().withNewMetadata().withName( "main" ).addToLabels( "group", "main" ).endMetadata().build();

        assertThat( operator.notLoaded( config( "all" ), List.of( neverUpdated ), List.of( pod( "main-0", "main", "10:00" ) ) ) ).isEmpty();
        assertThat( operator.notLoaded( config( "data" ), List.of( configMap( "main", "10:00" ) ), List.of() ) ).isEmpty();
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

    private static Pod pod( final String name, final String nodeGroup, final String reloadedAt )
    {
        return new PodBuilder().withNewMetadata()
            .withName( name )
            .addToLabels( "group", nodeGroup )
            .addToAnnotations( "reloaded", "2026-11-20T" + reloadedAt + ":00Z" )
            .endMetadata()
            .build();
    }

    private static Pod managed( final Pod pod )
    {
        return new PodBuilder( pod ).editMetadata().withNamespace( "ns" ).addToLabels( "enonic.managed", "true" ).endMetadata().build();
    }

    private static ConfigMap managed( final ConfigMap configMap )
    {
        return new ConfigMapBuilder( configMap ).editMetadata()
            .withNamespace( "ns" )
            .addToLabels( "enonic.managed", "true" )
            .endMetadata()
            .build();
    }

    private static ConfigMap configMap( final String nodeGroup, final String updatedAt )
    {
        return new ConfigMapBuilder().withNewMetadata()
            .withName( nodeGroup )
            .addToLabels( "group", nodeGroup )
            .addToAnnotations( "updated", "2026-11-20T" + updatedAt + ":00Z" )
            .endMetadata()
            .build();
    }

    private static Xp8Config config( final String nodeGroup )
    {
        final Xp8Config config = OperatorXp8ConfigUpdateTest.config( "a=1" );
        config.getSpec().setNodeGroup( nodeGroup );
        return config;
    }
}
