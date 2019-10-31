package com.enonic.ec.kubernetes.operator.commands.builders.spec;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Capabilities;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.HTTPGetAction;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpec;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.TCPSocketAction;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategy;

import com.enonic.ec.kubernetes.common.Configuration;

public abstract class StatefulSetSpecBase
    extends Configuration

{
    protected abstract Map<String, String> podLabels();

    protected abstract Map<String, String> podAnnotations();

    protected abstract String podImage();

    protected abstract String serviceName();

    protected abstract Integer replicas();

    protected abstract List<EnvVar> podEnv();

    protected abstract Map<String, Quantity> podResources();

    @Value.Derived
    public StatefulSetSpec spec()
    {
        StatefulSetSpec spec = new StatefulSetSpec();

        spec.setSelector( new LabelSelector( null, podLabels() ) );
        spec.setServiceName( serviceName() );
        spec.setReplicas( replicas() );
        spec.setPodManagementPolicy( cfgStr( "operator.deployment.xp.pod.managementPolicy" ) );
        spec.setUpdateStrategy( new StatefulSetUpdateStrategy( null, cfgStr( "operator.deployment.xp.pod.updateStrategy" ) ) );
        spec.setTemplate( createPodTemplateSpec() );
        spec.setVolumeClaimTemplates( createVolumeClaimTemplates() );

        return spec;
    }

    private PodTemplateSpec createPodTemplateSpec()
    {
        PodTemplateSpec podTemplateSpec = new PodTemplateSpec();

        ObjectMeta meta = new ObjectMeta();
        podTemplateSpec.setMetadata( meta );

        meta.setLabels( podLabels() );
        meta.setAnnotations( podAnnotations() );

        PodSpec podSpec = new PodSpec();
        podTemplateSpec.setSpec( podSpec );

        podSpec.setRestartPolicy( "Always" );

        PodSecurityContext securityContext = new PodSecurityContext();
        securityContext.setRunAsUser( cfgLong( "operator.deployment.xp.pod.runAsUser" ) );
        podSpec.setSecurityContext( securityContext );

        // TODO: Set pod affinity

        podSpec.setTerminationGracePeriodSeconds( cfgLong( "operator.deployment.xp.pod.gracePeriodSeconds" ) );
        podSpec.setInitContainers( createPodInitContainers() );
        podSpec.setContainers( createPodContainers() );
        podSpec.setVolumes( createVolumes() );

        return podTemplateSpec;
    }

    private List<Container> createPodInitContainers()
    {
        List<Container> res = new LinkedList<>();

        Container init = new Container();
        init.setName( "configure-sysctl" );
        init.setImage( podImage() );
        init.setImagePullPolicy( cfgStr( "operator.deployment.xp.pod.imagePullPolicy" ) );
        SecurityContext initSecurityContext = new SecurityContext();
        initSecurityContext.setRunAsUser( 0L );
        initSecurityContext.setPrivileged( true );
        init.setSecurityContext( initSecurityContext );
        init.setCommand( Arrays.asList( "sysctl", "-w", "vm.max_map_count=262144" ) );
        res.add( init );

        // This container makes sure that the dns records for the cluster are created
        // before xp tries to setup the cluster
        Container dnsWait = new Container();
        dnsWait.setName( "dns-wait" );
        dnsWait.setImage( "alpine:3.10" );
        dnsWait.setCommand( Arrays.asList( "ash", "-c", "while [ -z \"$(nslookup " + serviceName() +
            " | grep Name)\" ]; do echo \"Waiting for DNS\"; sleep 1; done" ) );
        res.add( dnsWait );

        return res;
    }

    private List<Container> createPodContainers()
    {
        // Basics
        Container exp = new Container();
        exp.setName( "exp" );
        exp.setImage( podImage() );
        exp.setImagePullPolicy( cfgStr( "operator.deployment.xp.pod.imagePullPolicy" ) );

        // Environment
        List<EnvVar> envVars = new LinkedList<>( podEnv() );

        EnvVar envNodeName = new EnvVar();
        envNodeName.setName( "XP_NODE_NAME" );
        envNodeName.setValueFrom( new EnvVarSource( null, new ObjectFieldSelector( null, "metadata.name" ), null, null ) );
        envVars.add( envNodeName );

        EnvVar envNodeIp = new EnvVar();
        envNodeIp.setName( "XP_NODE_IP" );
        envNodeIp.setValueFrom( new EnvVarSource( null, new ObjectFieldSelector( null, "status.podIP" ), null, null ) );
        envVars.add( envNodeIp );

        exp.setEnv( envVars );

        // Security
        SecurityContext podSecurityContext = new SecurityContext();
        Capabilities capabilities = new Capabilities();
        capabilities.setDrop( Collections.singletonList( "ALL" ) );
        podSecurityContext.setRunAsNonRoot( true );
        podSecurityContext.setRunAsUser( cfgLong( "operator.deployment.xp.pod.runAsUser" ) );
        podSecurityContext.setCapabilities( capabilities );
        exp.setSecurityContext( podSecurityContext );

        // Ports
        exp.setPorts( Arrays.asList( new ContainerPort( cfgInt( "operator.deployment.xp.port.stats.number" ), null, null,
                                                        cfgStr( "operator.deployment.xp.port.stats.name" ), null ),
                                     new ContainerPort( cfgInt( "operator.deployment.xp.port.main.number" ), null, null,
                                                        cfgStr( "operator.deployment.xp.port.main.name" ), null ),
                                     new ContainerPort( cfgInt( "operator.deployment.xp.port.es.http.number" ), null, null,
                                                        cfgStr( "operator.deployment.xp.port.es.http.name" ), null ),
                                     new ContainerPort( cfgInt( "operator.deployment.xp.port.es.discovery.number" ), null, null,
                                                        cfgStr( "operator.deployment.xp.port.es.discovery.name" ), null ) ) );

        // TODO: Add probe parameters to properties
        // Probes
        Probe readinessProbe = new Probe();
        exp.setReadinessProbe( readinessProbe );
        readinessProbe.setFailureThreshold( 10 );
        readinessProbe.setInitialDelaySeconds( 10 );
        readinessProbe.setPeriodSeconds( 5 );
        readinessProbe.setSuccessThreshold( 1 );
        readinessProbe.setTimeoutSeconds( 1 );
        readinessProbe.setTcpSocket( new TCPSocketAction( null, new IntOrString( cfgInt( "operator.deployment.xp.port.main.number" ) ) ) );

        Probe livelinessProbe = new Probe();
        exp.setLivenessProbe( livelinessProbe );
        livelinessProbe.setFailureThreshold( 10 );
        livelinessProbe.setInitialDelaySeconds( 5 );
        livelinessProbe.setPeriodSeconds( 20 );
        livelinessProbe.setSuccessThreshold( 1 );
        livelinessProbe.setTimeoutSeconds( 1 );
        livelinessProbe.setHttpGet( new HTTPGetAction( null, null, "/server", new IntOrString( "xp-stats" ), null ) );

        // Resources
        ResourceRequirements resourceRequirements = new ResourceRequirements();
        exp.setResources( resourceRequirements );
        resourceRequirements.setRequests( podResources() );
        resourceRequirements.setLimits( podResources() );

        // TODO: Lifecycle

        // Volume mounts
        exp.setVolumeMounts( createVolumeMounts() );

        return Collections.singletonList( exp );
    }

    @SuppressWarnings("SameParameterValue")
    static PersistentVolumeClaim standard( String name, Map<String, String> labels, String storageClassName, String accessMode,
                                           Quantity size )
    {
        PersistentVolumeClaim claim = new PersistentVolumeClaim();
        ObjectMeta meta = new ObjectMeta();
        claim.setMetadata( meta );
        meta.setName( name );
        meta.setLabels( labels );

        PersistentVolumeClaimSpec spec = new PersistentVolumeClaimSpec();
        claim.setSpec( spec );
        spec.setAccessModes( Collections.singletonList( accessMode ) );
        spec.setStorageClassName( storageClassName );
        spec.setResources( new ResourceRequirements( null, Map.of( "storage", size ) ) );

        return claim;
    }

    protected abstract List<VolumeMount> createVolumeMounts();

    protected abstract List<Volume> createVolumes();

    protected abstract List<PersistentVolumeClaim> createVolumeClaimTemplates();
}
