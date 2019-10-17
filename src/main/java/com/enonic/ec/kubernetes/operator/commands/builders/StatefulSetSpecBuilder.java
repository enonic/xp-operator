package com.enonic.ec.kubernetes.operator.commands.builders;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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

import com.enonic.ec.kubernetes.common.commands.Command;

public abstract class StatefulSetSpecBuilder
    implements Command<StatefulSetSpec>
{
    protected abstract Map<String, String> podLabels();

    protected abstract Map<String, String> extraPodLabels(); // TODO: Is this a good name

    protected abstract Map<String, String> podAnnotations();

    protected abstract String podImage();

    @Value.Default
    protected String podImagePullPolicy()
    {
        return "Always"; // TODO: Change
    }

    protected abstract String serviceName();

    protected abstract Integer replicas();

    protected abstract List<EnvVar> podEnv();

    protected abstract Map<String, Quantity> podResources();

    protected abstract String configMapName();

    static final String volumeRepo = "repo";

    protected static final String volumeIndex = volumeRepo + "index";

    protected static final String volumeBlob = volumeRepo + "blob";

    static final String volumeSnapshots = "snapshots";

    static final String volumeConfig = "config";

    static final String volumeDeploy = "deploy";

    @Override
    public StatefulSetSpec execute()
    {
        StatefulSetSpec spec = new StatefulSetSpec();

        spec.setSelector( new LabelSelector( null, podLabels() ) );
        spec.setServiceName( serviceName() );
        spec.setReplicas( replicas() );
        spec.setPodManagementPolicy( "Parallel" );
        spec.setUpdateStrategy( new StatefulSetUpdateStrategy( null, "RollingUpdate" ) );
        spec.setTemplate( createPodTemplateSpec() );
        spec.setVolumeClaimTemplates( createVolumeClaimTemplates() );

        return spec;
    }

    private PodTemplateSpec createPodTemplateSpec()
    {
        PodTemplateSpec podTemplateSpec = new PodTemplateSpec();

        ObjectMeta meta = new ObjectMeta();
        podTemplateSpec.setMetadata( meta );

        Map<String, String> combinedPodLabels = new HashMap<>();
        combinedPodLabels.putAll( podLabels() );
        combinedPodLabels.putAll( extraPodLabels() );
        meta.setLabels( combinedPodLabels );
        meta.setAnnotations( podAnnotations() );

        PodSpec podSpec = new PodSpec();
        podTemplateSpec.setSpec( podSpec );

        podSpec.setRestartPolicy( "Always" );

        PodSecurityContext securityContext = new PodSecurityContext();
        securityContext.setRunAsUser( 1337L );
        podSpec.setSecurityContext( securityContext );

        // TODO: Set pod affinity

        podSpec.setTerminationGracePeriodSeconds( 30L );
        podSpec.setInitContainers( createPodInitContainers() );
        podSpec.setContainers( createPodContainers() );
        podSpec.setVolumes( createVolumes() );

        return podTemplateSpec;
    }

    private List<Container> createPodInitContainers()
    {
        Container init = new Container();
        init.setName( "configure-sysctl" );
        init.setImage( podImage() );
        init.setImagePullPolicy( podImagePullPolicy() );
        SecurityContext initSecurityContext = new SecurityContext();
        initSecurityContext.setRunAsUser( 0L );
        initSecurityContext.setPrivileged( true );
        init.setSecurityContext( initSecurityContext );
        init.setCommand( Arrays.asList( "sysctl", "-w", "vm.max_map_count=262144" ) );
        return Collections.singletonList( init );
    }

    private List<Container> createPodContainers()
    {
        // Basics
        Container exp = new Container();
        exp.setName( "exp" );
        exp.setImage( podImage() );
        exp.setImagePullPolicy( podImagePullPolicy() );

        // Environment
        List<EnvVar> envVars = new LinkedList<>();
        envVars.addAll( podEnv() );

        EnvVar envNodeName = new EnvVar();
        envNodeName.setName( "XP_NODE_NAME" );
        envNodeName.setValueFrom( new EnvVarSource( null, new ObjectFieldSelector( null, "metadata.name" ), null, null ) );
        envVars.add( envNodeName );

        exp.setEnv( envVars );

        // Security
        SecurityContext podSecurityContext = new SecurityContext();
        Capabilities capabilities = new Capabilities();
        capabilities.setDrop( Collections.singletonList( "ALL" ) );
        podSecurityContext.setRunAsNonRoot( true );
        podSecurityContext.setRunAsUser( 1337L );
        podSecurityContext.setCapabilities( capabilities );
        exp.setSecurityContext( podSecurityContext );

        // Ports
        exp.setPorts( Arrays.asList( new ContainerPort( 8080, null, null, "xp-main", null ),
                                     new ContainerPort( 2609, null, null, "xp-stats", null ) ) );

        // Probes
        Probe readinessProbe = new Probe();
        exp.setReadinessProbe( readinessProbe );
        readinessProbe.setFailureThreshold( 3 );
        readinessProbe.setInitialDelaySeconds( 10 );
        readinessProbe.setPeriodSeconds( 5 );
        readinessProbe.setSuccessThreshold( 1 );
        readinessProbe.setTimeoutSeconds( 1 );
        readinessProbe.setTcpSocket( new TCPSocketAction( null, new IntOrString( "xp-main" ) ) );

        Probe livelinessProbe = new Probe();
        exp.setLivenessProbe( livelinessProbe );
        livelinessProbe.setFailureThreshold( 3 );
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

        // Lifecycle
        // TODO:

        // Volume mounts
        exp.setVolumeMounts( createVolumeMounts() );

        return Collections.singletonList( exp );
    }

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
