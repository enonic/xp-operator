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
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;

import com.enonic.cloud.kubernetes.Clients;
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
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderV1Alpha2Domain;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderV1Alpha2Xp7Config;
import com.enonic.cloud.kubernetes.commands.builders.ImmutableCommandBuilderV1Alpha2Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha1.xp7app.Xp7App;
import com.enonic.cloud.kubernetes.model.v1alpha2.domain.Domain;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7config.Xp7Config;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;

@SuppressWarnings("unchecked")
@Singleton
public class K8sCommandMapper
{
    private final Map<Class<? extends HasMetadata>, Function<GenericBuilderParams, Optional<K8sCommand>>> builderMap;

    private final Clients clients;

    @SuppressWarnings("CdiInjectionPointsInspection")

    @Inject
    public K8sCommandMapper( final Clients clients )
    {
        this.clients = clients;

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
        this.builderMap.put( Xp7App.class, this::v1alpha1Xp7App );
        this.builderMap.put( Domain.class, this::v1alpha2Domain );
        this.builderMap.put( Xp7Config.class, this::v1alpha2Xp7Config );
        this.builderMap.put( Xp7Deployment.class, this::v1alpha2Xp7Deployment );
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
            client( clients.k8s() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> daemonSet( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderDaemonSet.builder().
            client( clients.k8s() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> ingress( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderIngress.builder().
            client( clients.k8s() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> pvc( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderPVC.builder().
            client( clients.k8s() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> role( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderRole.builder().
            client( clients.k8s() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> roleBinding( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderRoleBinding.builder().
            client( clients.k8s() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> secret( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderSecret.builder().
            client( clients.k8s() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> service( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderService.builder().
            client( clients.k8s() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> serviceAccount( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderServiceAccount.builder().
            client( clients.k8s() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> statefulSet( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderStatefulSet.builder().
            client( clients.k8s() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> v1alpha1Xp7App( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderV1Alpha1Xp7App.builder().
            client( clients.xp7Apps() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> v1alpha2Domain( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderV1Alpha2Domain.builder().
            client( clients.domain() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> v1alpha2Xp7Config( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderV1Alpha2Xp7Config.builder().
            client( clients.xp7Configs() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> v1alpha2Xp7Deployment( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderV1Alpha2Xp7Deployment.builder().
            client( clients.xp7Deployments() ).
            build().
            apply( params );
    }
}
