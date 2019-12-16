package com.enonic.ec.kubernetes.operator.commands.deployments.spec;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.Capabilities;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.HTTPGetAction;
import io.fabric8.kubernetes.api.model.HTTPHeader;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategy;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.operator.commands.deployments.config.ClusterConfigurator;
import com.enonic.ec.kubernetes.operator.commands.deployments.volumes.VolumeTripletList;

@Value.Immutable
public abstract class StatefulSetSpecBuilder
    extends Configuration

{
    static Probe createProbe( String name )
    {
        Function<String, String> ck = k -> String.format( "operator.deployment.xp.probe.%s.%s", name, k );
        Probe p = new Probe();
        p.setHttpGet( new HTTPGetAction( null, Collections.singletonList(
            new HTTPHeader( "Host", cfgStr( "operator.deployment.xp.probe.healthcheck.host" ) ) ), cfgStr( ck.apply( "path" ) ),
                                         new IntOrString( cfgInt( ck.apply( "port" ) ) ), null ) );
        p.setInitialDelaySeconds( cfgInt( ck.apply( "initialDelaySeconds" ) ) );
        p.setPeriodSeconds( cfgInt( ck.apply( "periodSeconds" ) ) );
        p.setFailureThreshold( cfgInt( ck.apply( "failureThreshold" ) ) );
        p.setSuccessThreshold( cfgInt( ck.apply( "successThreshold" ) ) );
        p.setTimeoutSeconds( cfgInt( ck.apply( "timeoutSeconds" ) ) );
        return p;
    }

    protected abstract Map<String, String> podLabels();

    protected abstract Map<String, String> podAnnotations();

    protected abstract String podImage();

    protected abstract String serviceName();

    protected abstract Integer replicas();

    protected abstract List<EnvVar> podEnv();

    protected abstract Map<String, Quantity> podResources();

    protected abstract VolumeTripletList volumeList();

    protected abstract ClusterConfigurator clusterConfigurator();

    @Value.Derived
    protected Long runAsUser()
    {
        return cfgLong( "operator.deployment.xp.pod.runAsUser" );
    }

    @Value.Derived
    public io.fabric8.kubernetes.api.model.apps.StatefulSetSpec spec()
    {
        io.fabric8.kubernetes.api.model.apps.StatefulSetSpec spec = new io.fabric8.kubernetes.api.model.apps.StatefulSetSpec();

        spec.setSelector( new LabelSelector( null, podLabels() ) );
        spec.setServiceName( serviceName() );
        spec.setReplicas( replicas() );
        spec.setPodManagementPolicy( cfgStr( "operator.deployment.xp.pod.managementPolicy" ) );
        spec.setUpdateStrategy( new StatefulSetUpdateStrategy( null, cfgStr( "operator.deployment.xp.pod.updateStrategy" ) ) );
        spec.setTemplate( createPodTemplateSpec() );
        spec.setVolumeClaimTemplates( volumeList().volumeClaimTemplates() );

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
        securityContext.setFsGroup( cfgLong( "operator.deployment.xp.pod.fsGroup" ) );
        podSpec.setSecurityContext( securityContext );

        // TODO: Set pod affinity

        podSpec.setTerminationGracePeriodSeconds( cfgLong( "operator.deployment.xp.pod.gracePeriodSeconds" ) );
        podSpec.setInitContainers( createPodInitContainers() );
        podSpec.setContainers( createPodContainers() );
        podSpec.setVolumes( volumeList().volumes() );

        return podTemplateSpec;
    }

    private List<Container> createPodInitContainers()
    {
        List<Container> res = new LinkedList<>();

        // Set virtual memory for elastic
        // https://www.elastic.co/guide/en/elasticsearch/reference/2.4/setup-configuration.html#vm-max-map-count
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
        // TODO: Only use 1 container
        for ( int i = 0; i < clusterConfigurator().waitForDnsRecords().size(); i++ )
        {
            String host = clusterConfigurator().waitForDnsRecords().get( i );
            Container dnsWait = new Container();
            dnsWait.setName( "dns-wait-" + i );
            dnsWait.setImage( "alpine:3.10" );
            dnsWait.setCommand( Arrays.asList( "ash", "-c", "while [ -z \"$(nslookup " + host +
                " | grep Name)\" ]; do echo \"Waiting for DNS\"; sleep 1; done" ) );
            res.add( dnsWait );
        }

        // Set permissions for NFS shares
        List<Volume> volumes = volumeList().volumes();
        Optional<Volume> nfs = volumes.stream().filter( v -> v.getNfs() != null ).findFirst();
        if ( nfs.isPresent() )
        {
            List<VolumeMount> nfsMounts = volumeList().volumeMounts().stream().
                filter( m -> m.getName().equals( nfs.get().getName() ) ).
                collect( Collectors.toList() );

            Container nfsPermissions = new Container();
            nfsPermissions.setName( "nfs-permissions" );
            nfsPermissions.setImage( "alpine:3.10" );
            nfsPermissions.setSecurityContext( initSecurityContext );
            nfsPermissions.setVolumeMounts( nfsMounts );

            List<String> chownCommands = nfsMounts.stream().
                map( m -> String.format( "chown %s:0 %s", runAsUser(), m.getMountPath() ) ).
                collect( Collectors.toList() );

            nfsPermissions.setCommand( Arrays.asList( "ash", "-c", String.join( " && ", chownCommands ) ) );
            res.add( nfsPermissions );
        }

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
        podSecurityContext.setRunAsUser( runAsUser() );
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

        // Probes
        exp.setReadinessProbe( createProbe( "readiness" ) );
        exp.setLivenessProbe( createProbe( "liveliness" ) );

        // Resources
        ResourceRequirements resourceRequirements = new ResourceRequirements();
        exp.setResources( resourceRequirements );
        resourceRequirements.setRequests( podResources() );
        resourceRequirements.setLimits( podResources() );

        // TODO: Lifecycle

        // Volume mounts
        exp.setVolumeMounts( volumeList().volumeMounts() );

        return Collections.singletonList( exp );
    }
}
