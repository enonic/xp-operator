package com.enonic.cloud.operator.kubectl;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;

import com.enonic.cloud.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.operator.kubectl.base.KubeCommandBuilder;
import com.enonic.cloud.operator.kubectl.base.KubeCommandOptions;
import com.enonic.cloud.operator.operators.common.clients.Clients;

class CommandMapper
{
    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unchecked"})
    static <T extends HasMetadata> KubeCommandBuilder<T> getCommandClass( Clients clients, Optional<String> namespace, T resource,
                                                                          KubeCommandOptions options )
    {
        if ( resource instanceof ConfigMap )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdConfigMaps.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (ConfigMap) resource ).
                options( options ).
                build();
        }
        else if ( resource instanceof DaemonSet )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdDaemonSets.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (DaemonSet) resource ).
                options( options ).
                build();
        }
        else if ( resource instanceof Ingress )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdIngresses.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (Ingress) resource ).
                options( options ).
                build();
        }
        else if ( resource instanceof Namespace )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdNamespaces.builder().
                clients( clients ).
                resource( (Namespace) resource ).
                options( options ).
                build();
        }
        else if ( resource instanceof Role )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdRole.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (Role) resource ).
                options( options ).
                build();
        }
        else if ( resource instanceof RoleBinding )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdRoleBinding.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (RoleBinding) resource ).
                options( options ).
                build();
        }
        else if ( resource instanceof PersistentVolumeClaim )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdPVCs.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (PersistentVolumeClaim) resource ).
                options( options ).
                build();
        }
        else if ( resource instanceof Secret )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdSecrets.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (Secret) resource ).
                options( options ).
                build();
        }
        else if ( resource instanceof Service )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdServices.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (Service) resource ).
                options( options ).
                build();
        }
        else if ( resource instanceof StatefulSet )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdStatefulSets.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (StatefulSet) resource ).
                options( options ).
                build();
        }
        else if ( resource instanceof V1alpha1Xp7App )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdV1alpha1Xp7Apps.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (V1alpha1Xp7App) resource ).
                options( options ).
                build();
        }
        else if ( resource instanceof V1alpha2Xp7Config )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdV1alpha2Xp7Configs.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (V1alpha2Xp7Config) resource ).
                options( options ).
                build();
        }
        else if ( resource instanceof V1alpha2Xp7VHost )
        {
            return (KubeCommandBuilder<T>) ImmutableKubeCmdV1alpha2Xp7VHosts.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (V1alpha2Xp7VHost) resource ).
                options( options ).
                build();
        }
        throw new IllegalStateException( "Cannot find command class for " + resource.getKind() );
    }
}
