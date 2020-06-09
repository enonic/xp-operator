package com.enonic.cloud.kubernetes.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;

import com.enonic.cloud.kubernetes.commands.builders.GenericBuilderParams;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderConfigMap;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderDaemonSet;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderIngress;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderPVC;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderRole;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderRoleBinding;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderSecret;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderService;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderServiceAccount;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderStatefulSet;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderV1Alpha1Xp7App;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderV1Alpha2Xp7Config;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderV1Alpha2Xp7Deployment;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderV1Alpha2Xp7VHost;
import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;

@SuppressWarnings("unchecked")
@Singleton
public class K8sCommandMapper
{
    private final Map<Class<? extends HasMetadata>, Function<GenericBuilderParams, Optional<K8sCommand>>> builderMap;

    private final KubernetesClient kubernetesClient;

    private final CrdClient crdClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public K8sCommandMapper( final KubernetesClient kubernetesClient, final CrdClient crdClient )
    {
        this.kubernetesClient = kubernetesClient;
        this.crdClient = crdClient;

        this.builderMap = new HashMap<>();
        this.builderMap.put( ConfigMap.class, this::configMap );
        this.builderMap.put( DaemonSet.class, this::daemonSet );
        this.builderMap.put( Ingress.class, this::ingress );
        this.builderMap.put( PersistentVolumeClaim.class, this::pvc );
        this.builderMap.put( Role.class, this::role );
        this.builderMap.put( RoleBinding.class, this::roleBinding );
        this.builderMap.put( Secret.class, this::secret );
        this.builderMap.put( Service.class, this::service );
        this.builderMap.put( ServiceAccount.class, this::serviceAccount );
        this.builderMap.put( StatefulSet.class, this::statefulSet );
        this.builderMap.put( V1alpha1Xp7App.class, this::v1alpha1Xp7App );
        this.builderMap.put( V1alpha2Xp7Config.class, this::v1alpha2Xp7Config );
        this.builderMap.put( V1alpha2Xp7Deployment.class, this::v1alpha2Xp7Deployment );
        this.builderMap.put( V1alpha2Xp7VHost.class, this::v1alpha2Xp7VHost );
    }

    public Optional<K8sCommand> getCommand( final GenericBuilderParams params )
    {
        Function<GenericBuilderParams, Optional<K8sCommand>> builderFunc = this.builderMap.get( params.resource().getClass() );
        Preconditions.checkState( builderFunc != null, String.format( "CommandBuilder for class '%s' not found",
                                                                      params.resource().getClass().getSimpleName() ) );
        return builderFunc.apply( params );
    }

    private Optional<K8sCommand> configMap( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderConfigMap.builder().
            client( kubernetesClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> daemonSet( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderDaemonSet.builder().
            client( kubernetesClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> ingress( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderIngress.builder().
            client( kubernetesClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> pvc( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderPVC.builder().
            client( kubernetesClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> role( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderRole.builder().
            client( kubernetesClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> roleBinding( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderRoleBinding.builder().
            client( kubernetesClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> secret( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderSecret.builder().
            client( kubernetesClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> service( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderService.builder().
            client( kubernetesClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> serviceAccount( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderServiceAccount.builder().
            client( kubernetesClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> statefulSet( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderStatefulSet.builder().
            client( kubernetesClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> v1alpha1Xp7App( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderV1Alpha1Xp7App.builder().
            client( crdClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> v1alpha2Xp7Config( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderV1Alpha2Xp7Config.builder().
            client( crdClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> v1alpha2Xp7Deployment( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderV1Alpha2Xp7Deployment.builder().
            client( crdClient ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> v1alpha2Xp7VHost( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderV1Alpha2Xp7VHost.builder().
            client( crdClient ).
            build().
            apply( params );
    }
}
