package com.enonic.ec.kubernetes.operator.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategy;
import io.fabric8.kubernetes.client.KubernetesClient;
import jdk.jshell.spi.ExecutionControl;

import com.enonic.ec.kubernetes.common.commands.Command;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

public class CommandApplyStatefulSet
    implements Command<StatefulSet>
{
    private final Logger log = LoggerFactory.getLogger( CommandApplyStatefulSet.class );

    private final KubernetesClient client;

    private final OwnerReference ownerReference;

    private final String name;

    private final String namespace;

    private final Map<String, String> labels;

    private final String image;

    private final String imagePullPolicy = "Always";

    private final Integer replicas;

    private final String podManagementPolicy;

    private final StatefulSetUpdateStrategy statefulSetUpdateStrategy;

    private final Map<String, String> podAnnotations;

    private final Long runAsUser;

    private final Long podTerminationGracePeriodSeconds;

    private CommandApplyStatefulSet( final Builder builder )
    {
        client = assertNotNull( "client", builder.client );
        ownerReference = assertNotNull( "ownerReference", builder.ownerReference );
        name = assertNotNull( "name", builder.name );
        namespace = assertNotNull( "namespace", builder.namespace );
        labels = assertNotNull( "labels", builder.labels );
        image = builder.image;
        replicas = builder.replicas;
        podManagementPolicy = builder.podManagementPolicy;
        statefulSetUpdateStrategy = builder.statefulSetUpdateStrategy;
        podAnnotations = builder.podAnnotations;
        runAsUser = builder.runAsUser;
        podTerminationGracePeriodSeconds = builder.podTerminationGracePeriodSeconds;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @Override
    public StatefulSet execute()
        throws ExecutionControl.NotImplementedException
    {
        log.debug( "Creating in Namespace '" + namespace + "' Statefulset '" + name + "'" );
        throw new ExecutionControl.NotImplementedException( "Not Implemented" );

//        StatefulSet statefulSet = new StatefulSet();
//        statefulSet.setMetadata( createStatefulSetMetadata() );
//        statefulSet.setSpec( createStatefulSetSpec() );
//
//        return client.apps().statefulSets().inNamespace( namespace ).createOrReplace( statefulSet );
    }

    private ObjectMeta createStatefulSetMetadata()
    {
        ObjectMeta metaData = new ObjectMeta();
        metaData.setOwnerReferences( List.of( ownerReference ) );
        metaData.setName( name );
        metaData.setNamespace( namespace );
        metaData.setLabels( labels );
        return metaData;
    }

    private StatefulSetSpec createStatefulSetSpec()
    {
        StatefulSetSpec spec = new StatefulSetSpec();
        spec.setSelector( new LabelSelector( null, labels ) );
        spec.setServiceName( name );
        spec.setReplicas( replicas );
        spec.setPodManagementPolicy( podManagementPolicy );
        spec.setUpdateStrategy( statefulSetUpdateStrategy );
        spec.setTemplate( createPodSpec() );
        spec.setVolumeClaimTemplates( createVolumeClaimTemplates() );
        return spec;
    }

    private PodTemplateSpec createPodSpec()
    {
        PodTemplateSpec podTemplateSpec = new PodTemplateSpec();
        ObjectMeta podMetadata = new ObjectMeta();
        podMetadata.setLabels( labels );
        podMetadata.setAnnotations( podAnnotations );
        podTemplateSpec.setMetadata( podMetadata );

        // Create spec/podTemplateSpec/spec
        PodSpec podSpec = new PodSpec();
        podSpec.setRestartPolicy( "Always" );
        podSpec.setSecurityContext( new PodSecurityContext( null, null, null, runAsUser, null, null, null, null ) );

        // TODO: Set pod affinity
        podSpec.setTerminationGracePeriodSeconds( podTerminationGracePeriodSeconds );

        // Create spec/podTemplateSpec/spec
        Container configureSysctl = new Container();
        configureSysctl.setImage( image );
        configureSysctl.setImagePullPolicy( imagePullPolicy );

        podSpec.setInitContainers( Arrays.asList( configureSysctl ) );

        return podTemplateSpec;
    }

    private List<PersistentVolumeClaim> createVolumeClaimTemplates()
    {
        return null;
    }

    public static final class Builder
    {
        private KubernetesClient client;

        private OwnerReference ownerReference;

        private String name;

        private String namespace;

        private Map<String, String> labels;

        private String image;

        private Integer replicas;

        private String podManagementPolicy;

        private StatefulSetUpdateStrategy statefulSetUpdateStrategy;

        private Map<String, String> podAnnotations;

        private Long runAsUser;

        private Long podTerminationGracePeriodSeconds;

        private Map<String, String> data;

        private Builder()
        {
        }

        public Builder client( final KubernetesClient val )
        {
            client = val;
            return this;
        }

        public Builder ownerReference( final OwnerReference val )
        {
            ownerReference = val;
            return this;
        }

        public Builder name( final String val )
        {
            name = val;
            return this;
        }

        public Builder namespace( final String val )
        {
            namespace = val;
            return this;
        }

        public Builder labels( final Map<String, String> val )
        {
            labels = val;
            return this;
        }

        public Builder image( final String val )
        {
            image = val;
            return this;
        }

        public Builder replicas( final Integer val )
        {
            replicas = val;
            return this;
        }

        public Builder podManagementPolicy( final String val )
        {
            podManagementPolicy = val;
            return this;
        }

        public Builder statefulSetUpdateStrategy( final StatefulSetUpdateStrategy val )
        {
            statefulSetUpdateStrategy = val;
            return this;
        }

        public Builder podAnnotations( final Map<String, String> val )
        {
            podAnnotations = val;
            return this;
        }

        public Builder runAsUser( final Long val )
        {
            runAsUser = val;
            return this;
        }

        public Builder podTerminationGracePeriodSeconds( final Long val )
        {
            podTerminationGracePeriodSeconds = val;
            return this;
        }

        public Builder data( final Map<String, String> val )
        {
            data = val;
            return this;
        }

        public CommandApplyStatefulSet build()
        {
            return new CommandApplyStatefulSet( this );
        }
    }
}
