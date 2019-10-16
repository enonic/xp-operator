package com.enonic.ec.kubernetes.operator.builders;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Capabilities;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
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

@Value.Immutable
public abstract class StatefulSetSpecBuilder
    implements Command<StatefulSetSpec>
{
    protected abstract Map<String, String> podLabels();

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

    private static final String volumePostfixIndex = "-repo-index";

    private static final String volumePostfixBlob = "-repo-blob";

    private static final String volumePostfixSnapshots = "-snapshots";

    private static final String volumePostfixConfig = "-config";

    private static final String volumePostfixDeploy = "-deploy";

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
        meta.setLabels( podLabels() );
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
        return Arrays.asList( init );
    }

    private List<Container> createPodContainers()
    {
        // Basics
        Container exp = new Container();
        exp.setName( "exp" );
        exp.setImage( podImage() );
        exp.setImagePullPolicy( podImagePullPolicy() );

        // Environment
        EnvVar envNodeName = new EnvVar();
        envNodeName.setName( "XP_NODE_NAME" );
        envNodeName.setValueFrom( new EnvVarSource( null, new ObjectFieldSelector( null, "metadata.name" ), null, null ) );
        podEnv().add( envNodeName );
        exp.setEnv( podEnv() );

        // Security
        SecurityContext podSecurityContext = new SecurityContext();
        Capabilities capabilities = new Capabilities();
        capabilities.setDrop( Arrays.asList( "ALL" ) );
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

        Probe livenessProbe = new Probe();
        exp.setLivenessProbe( livenessProbe );
        livenessProbe.setFailureThreshold( 3 );
        livenessProbe.setInitialDelaySeconds( 5 );
        livenessProbe.setPeriodSeconds( 20 );
        livenessProbe.setSuccessThreshold( 3 );
        livenessProbe.setTimeoutSeconds( 1 );
        livenessProbe.setHttpGet( new HTTPGetAction( null, null, "/server", new IntOrString( "xp-stats" ), null ) );

        // Resources
        ResourceRequirements resourceRequirements = new ResourceRequirements();
        exp.setResources( resourceRequirements );
        resourceRequirements.setRequests( podResources() );
        resourceRequirements.setLimits( podResources() );

        // Lifecycle
        // TODO:

        // Volume mounts
        List<VolumeMount> volumeMounts = new LinkedList<>();
        exp.setVolumeMounts( volumeMounts );
        volumeMounts.add( new VolumeMount( "/enonic-xp/home/repo/index", null, serviceName() + volumePostfixIndex, null, null, null ) );
        volumeMounts.add( new VolumeMount( "/enonic-xp/home/repo/blob", null, serviceName() + volumePostfixBlob, null, null, null ) );
        volumeMounts.add( new VolumeMount( "/enonic-xp/home/snapshots", null, serviceName() + volumePostfixSnapshots, null, null, null ) );
        volumeMounts.add( new VolumeMount( "/enonic-xp/home/config", null, serviceName() + volumePostfixConfig, null, null, null ) );
        volumeMounts.add( new VolumeMount( "/enonic-xp/home/deploy", null, serviceName() + volumePostfixDeploy, null, null, null ) );

        return Arrays.asList( exp );
    }

    private List<Volume> createVolumes()
    {
        Volume configMap = new Volume();
        configMap.setName( serviceName() + volumePostfixConfig );
        configMap.setConfigMap( new ConfigMapVolumeSource( null, null, configMapName(), null ) );

        Volume deploy = new Volume();
        deploy.setName( serviceName() + volumePostfixDeploy );
        deploy.setEmptyDir( new EmptyDirVolumeSource( null, null ) );

        return Arrays.asList( configMap, deploy );
    }

    private List<PersistentVolumeClaim> createVolumeClaimTemplates()
    {
        return Arrays.asList( standard( serviceName() + volumePostfixIndex, podLabels(), null, "ReadWriteOnce", new Quantity( "5Gi" ) ),
                              standard( serviceName() + volumePostfixBlob, podLabels(), null, "ReadWriteOnce", new Quantity( "5Gi" ) ),
                              standard( serviceName() + volumePostfixSnapshots, podLabels(), null, "ReadWriteOnce",
                                        new Quantity( "5Gi" ) ) );
    }

    private static PersistentVolumeClaim standard( String name, Map<String, String> labels, String storageClassName, String accessMode,
                                                   Quantity size )
    {
        PersistentVolumeClaim claim = new PersistentVolumeClaim();

        ObjectMeta meta = new ObjectMeta();
        claim.setMetadata( meta );
        meta.setName( name );
        meta.setLabels( labels );

        PersistentVolumeClaimSpec spec = new PersistentVolumeClaimSpec();
        claim.setSpec( spec );
        spec.setAccessModes( Arrays.asList( accessMode ) );
        spec.setStorageClassName( storageClassName );
        spec.setResources( new ResourceRequirements( null, Map.of( "storage", size ) ) );

        return claim;
    }
}
