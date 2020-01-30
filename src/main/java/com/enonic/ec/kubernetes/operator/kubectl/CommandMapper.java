package com.enonic.ec.kubernetes.operator.kubectl;

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

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.ec.kubernetes.operator.kubectl.base.KubeCommandResource;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;

class CommandMapper
{
    @SuppressWarnings({"unchecked", "OptionalUsedAsFieldOrParameterType"})
    static <T extends HasMetadata> KubeCommandResource<T> getCommandClass( Clients clients, Optional<String> namespace, T resource )
    {
        if ( resource instanceof ConfigMap )
        {
            return (KubeCommandResource<T>) ImmutableKubeCmdConfigMaps.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (ConfigMap) resource ).
                build();
        }
        else if ( resource instanceof DaemonSet )
        {
            return (KubeCommandResource<T>) ImmutableKubeCmdDaemonSets.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (DaemonSet) resource ).
                build();
        }
        else if ( resource instanceof Ingress )
        {
            return (KubeCommandResource<T>) ImmutableKubeCmdIngresses.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (Ingress) resource ).
                build();
        }
        else if ( resource instanceof Namespace )
        {
            return (KubeCommandResource<T>) ImmutableKubeCmdNamespaces.builder().
                clients( clients ).
                resource( (Namespace) resource ).
                build();
        }
        else if ( resource instanceof PersistentVolumeClaim )
        {
            return (KubeCommandResource<T>) ImmutableKubeCmdPVCs.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (PersistentVolumeClaim) resource ).
                build();
        }
        else if ( resource instanceof Secret )
        {
            return (KubeCommandResource<T>) ImmutableKubeCmdSecrets.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (Secret) resource ).
                build();
        }
        else if ( resource instanceof Service )
        {
            return (KubeCommandResource<T>) ImmutableKubeCmdServices.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (Service) resource ).
                build();
        }
        else if ( resource instanceof StatefulSet )
        {
            return (KubeCommandResource<T>) ImmutableKubeCmdStatefulSets.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (StatefulSet) resource ).
                build();
        }
        else if ( resource instanceof V1alpha1Xp7App )
        {
            return (KubeCommandResource<T>) ImmutableKubeCmdV1alpha1Xp7Apps.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (V1alpha1Xp7App) resource ).
                build();
        }
        else if ( resource instanceof V1alpha2Xp7Config )
        {
            return (KubeCommandResource<T>) ImmutableKubeCmdV1alpha2Xp7Configs.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (V1alpha2Xp7Config) resource ).
                build();
        }
        else if ( resource instanceof V1alpha2Xp7VHost )
        {
            return (KubeCommandResource<T>) ImmutableKubeCmdV1alpha2Xp7VHosts.builder().
                clients( clients ).
                namespace( namespace ).
                resource( (V1alpha2Xp7VHost) resource ).
                build();
        }
        throw new IllegalStateException( "Cannot find command class for " + resource.getKind() );
    }
}
