package com.enonic.kubernetes.kubernetes.commands;

import com.enonic.kubernetes.client.v1.xp7app.Xp7App;
import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.client.v1.xp7config.Xp7Config;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.commands.builders.GenericBuilderParams;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderClusterRole;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderClusterRoleBinding;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderConfigMap;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderDaemonSet;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderIngress;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderPVC;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderRole;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderRoleBinding;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderSecret;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderService;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderServiceAccount;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderStatefulSet;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderXp7App;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderDomain;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderXp7Config;
import com.enonic.kubernetes.kubernetes.commands.builders.ImmutableCommandBuilderXp7Deployment;
import com.google.common.base.Preconditions;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;


@SuppressWarnings("unchecked")
@Singleton
public class K8sCommandMapper
{
    private final Map<Class<? extends HasMetadata>, Function<GenericBuilderParams, Optional<K8sCommand>>> builderMap;

    private final Clients clients;

    @Inject
    public K8sCommandMapper( final Clients clients )
    {
        singletonAssert( this, "constructor" );

        this.clients = clients;

        this.builderMap = new HashMap<>();
        this.builderMap.put( ClusterRole.class, this::clusterRole );
        this.builderMap.put( ClusterRoleBinding.class, this::clusterRoleBinding );
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
        this.builderMap.put( Xp7App.class, this::Xp7App);
        this.builderMap.put( Domain.class, this::Domain);
        this.builderMap.put( Xp7Config.class, this::Xp7Config);
        this.builderMap.put( Xp7Deployment.class, this::Xp7Deployment);
    }

    public Optional<K8sCommand> getCommand( final GenericBuilderParams params )
    {
        Function<GenericBuilderParams, Optional<K8sCommand>> builderFunc = this.builderMap.get( params.resource().getClass() );
        Preconditions.checkState( builderFunc != null, String.format( "CommandBuilder for class '%s' not found",
            params.resource().getClass().getSimpleName() ) );
        return builderFunc.apply( params );
    }

    private Optional<K8sCommand> clusterRole( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderClusterRole.builder().
            client( clients.k8s() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> clusterRoleBinding( final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderClusterRoleBinding.builder().
            client( clients.k8s() ).
            build().
            apply( params );
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

    private Optional<K8sCommand> Xp7App(final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderXp7App.builder().
            client( clients.xp7Apps() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> Domain(final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderDomain.builder().
            client( clients.domain() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> Xp7Config(final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderXp7Config.builder().
            client( clients.xp7Configs() ).
            build().
            apply( params );
    }

    private Optional<K8sCommand> Xp7Deployment(final GenericBuilderParams params )
    {
        return ImmutableCommandBuilderXp7Deployment.builder().
            client( clients.xp7Deployments() ).
            build().
            apply( params );
    }
}
