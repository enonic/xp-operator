package com.enonic.kubernetes.kubernetes.commands;

import com.enonic.kubernetes.crd.v1.Xp8Config;
import com.enonic.kubernetes.crd.v1.Xp8Deployment;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.commands.builders.GenericBuilderParams;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderClusterRole;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderClusterRoleBinding;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderConfigMap;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderDaemonSet;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderIngress;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderPVC;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderRole;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderRoleBinding;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderSecret;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderService;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderServiceAccount;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderStatefulSet;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderXp8Config;
import com.enonic.kubernetes.kubernetes.commands.builders.CommandBuilderXp8Deployment;
import com.enonic.kubernetes.common.Preconditions;
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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
        this.builderMap.put( Xp8Config.class, this::Xp8Config);
        this.builderMap.put( Xp8Deployment.class, this::Xp8Deployment);
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
        return new CommandBuilderClusterRole( clients.k8s() ).apply( params );
    }

    private Optional<K8sCommand> clusterRoleBinding( final GenericBuilderParams params )
    {
        return new CommandBuilderClusterRoleBinding( clients.k8s() ).apply( params );
    }

    private Optional<K8sCommand> configMap( final GenericBuilderParams params )
    {
        return new CommandBuilderConfigMap( clients.k8s() ).apply( params );
    }

    private Optional<K8sCommand> daemonSet( final GenericBuilderParams params )
    {
        return new CommandBuilderDaemonSet( clients.k8s() ).apply( params );
    }

    private Optional<K8sCommand> ingress( final GenericBuilderParams params )
    {
        return new CommandBuilderIngress( clients.k8s() ).apply( params );
    }

    private Optional<K8sCommand> pvc( final GenericBuilderParams params )
    {
        return new CommandBuilderPVC( clients.k8s() ).apply( params );
    }

    private Optional<K8sCommand> role( final GenericBuilderParams params )
    {
        return new CommandBuilderRole( clients.k8s() ).apply( params );
    }

    private Optional<K8sCommand> roleBinding( final GenericBuilderParams params )
    {
        return new CommandBuilderRoleBinding( clients.k8s() ).apply( params );
    }

    private Optional<K8sCommand> secret( final GenericBuilderParams params )
    {
        return new CommandBuilderSecret( clients.k8s() ).apply( params );
    }

    private Optional<K8sCommand> service( final GenericBuilderParams params )
    {
        return new CommandBuilderService( clients.k8s() ).apply( params );
    }

    private Optional<K8sCommand> serviceAccount( final GenericBuilderParams params )
    {
        return new CommandBuilderServiceAccount( clients.k8s() ).apply( params );
    }

    private Optional<K8sCommand> statefulSet( final GenericBuilderParams params )
    {
        return new CommandBuilderStatefulSet( clients.k8s() ).apply( params );
    }

    private Optional<K8sCommand> Xp8Config(final GenericBuilderParams params )
    {
        return new CommandBuilderXp8Config( clients.xp8Configs() ).apply( params );
    }

    private Optional<K8sCommand> Xp8Deployment(final GenericBuilderParams params )
    {
        return new CommandBuilderXp8Deployment( clients.xp8Deployments() ).apply( params );
    }
}
