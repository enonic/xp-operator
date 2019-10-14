//package com.enonic.ec.kubernetes.operator.commands;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import io.fabric8.kubernetes.api.model.ConfigMap;
//import io.fabric8.kubernetes.api.model.Container;
//import io.fabric8.kubernetes.api.model.LabelSelector;
//import io.fabric8.kubernetes.api.model.LabelSelectorRequirementBuilder;
//import io.fabric8.kubernetes.api.model.ObjectMeta;
//import io.fabric8.kubernetes.api.model.OwnerReference;
//import io.fabric8.kubernetes.api.model.PodSecurityContext;
//import io.fabric8.kubernetes.api.model.PodSpec;
//import io.fabric8.kubernetes.api.model.PodTemplate;
//import io.fabric8.kubernetes.api.model.PodTemplateSpec;
//import io.fabric8.kubernetes.api.model.apps.StatefulSet;
//import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
//import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategy;
//import io.fabric8.kubernetes.client.KubernetesClient;
//
//import com.enonic.ec.kubernetes.common.commands.Command;
//
//import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;
//import static com.enonic.ec.kubernetes.common.assertions.Assertions.ifNullDefault;
//
//public class CommandCreateStatefulSet
//    implements Command<ConfigMap>
//{
//    private final Logger log = LoggerFactory.getLogger( CommandCreateStatefulSet.class );
//
//    private final KubernetesClient client;
//
//    private final OwnerReference ownerReference;
//
//    private final String name;
//
//    private final String namespace;
//
//    private final Map<String, String> labels;
//
//    private final String image;
//
//    private final String imagePullPolicy = "Always";
//
//    private final Integer replicas;
//
//    private final String podManagementPolicy;
//
//    private final StatefulSetUpdateStrategy statefulSetUpdateStrategy;
//
//    private final Map<String, String> podAnnotations;
//
//    private final Long runAsUser;
//
//    private final Long podTerminationGracePeriodSeconds;
//
//    private CommandCreateStatefulSet( final Builder builder )
//    {
//        client = assertNotNull( "client", builder.client );
//        ownerReference = assertNotNull( "ownerReference", builder.ownerReference );
//        name = assertNotNull( "name", builder.name );
//        namespace = assertNotNull( "namespace", builder.namespace );
//        labels = assertNotNull( "labels", builder.labels );
//    }
//
//    public static Builder newBuilder()
//    {
//        return new Builder();
//    }
//
//    @Override
//    public ConfigMap execute()
//    {
//        log.debug( "Creating in Namespace '" + namespace + "' Statefulset '" + name + "'" );
//        StatefulSet statefulSet = new StatefulSet(  );
//
//        // Create metadata
//        ObjectMeta metaData = new ObjectMeta();
//        metaData.setOwnerReferences( List.of( ownerReference ) );
//        metaData.setName( name );
//        metaData.setNamespace( namespace );
//        metaData.setLabels( labels );
//        statefulSet.setMetadata( metaData );
//
//        // Create spec
//        StatefulSetSpec spec = new StatefulSetSpec(  );
//        spec.setSelector( new LabelSelector( null,  labels) );
//        spec.setServiceName( name );
//        spec.setReplicas( replicas );
//        spec.setPodManagementPolicy( podManagementPolicy );
//        spec.setUpdateStrategy( statefulSetUpdateStrategy );
//
//        // Create spec/podTemplateSpec
//        PodTemplateSpec podTemplateSpec = new PodTemplateSpec(  );
//        ObjectMeta podMetadata = new ObjectMeta(  );
//        podMetadata.setLabels( labels );
//        podMetadata.setAnnotations( podAnnotations );
//        podTemplateSpec.setMetadata( podMetadata );
//
//        // Create spec/podTemplateSpec/spec
//        PodSpec podSpec = new PodSpec(  );
//        podSpec.setRestartPolicy( "Always" );
//        podSpec.setSecurityContext( new PodSecurityContext( null, null, null, runAsUser, null, null, null, null ) );
//
//        // TODO: Set pod affinity
//        podSpec.setTerminationGracePeriodSeconds( podTerminationGracePeriodSeconds );
//
//        // Create spec/podTemplateSpec/spec
//        Container configureSysctl = new Container(  );
//        configureSysctl.setImage( image );
//        configureSysctl.setImagePullPolicy( imagePullPolicy );
//
//        podSpec.setInitContainers( Arrays.asList(configureSysctl) );
//
//
//
//        podTemplateSpec.setSpec( podSpec );
//        spec.setTemplate( podTemplateSpec );
//
//        // Create spec/volumeClaimTemplates
//        spec.setVolumeClaimTemplates(  );
//
//
//        return client.configMaps().inNamespace( namespace ).createOrReplace( configMap );
//    }
//
//    public static final class Builder
//    {
//        private KubernetesClient client;
//
//        private OwnerReference ownerReference;
//
//        private String name;
//
//        private String namespace;
//
//        private Map<String, String> labels;
//
//        private Map<String, String> data;
//
//        private Builder()
//        {
//        }
//
//        public Builder client( final KubernetesClient val )
//        {
//            client = val;
//            return this;
//        }
//
//        public Builder ownerReference( final OwnerReference val )
//        {
//            ownerReference = val;
//            return this;
//        }
//
//        public Builder name( final String val )
//        {
//            name = val;
//            return this;
//        }
//
//        public Builder namespace( final String val )
//        {
//            namespace = val;
//            return this;
//        }
//
//        public Builder labels( final Map<String, String> val )
//        {
//            labels = val;
//            return this;
//        }
//
//        public Builder data( final Map<String, String> val )
//        {
//            data = val;
//            return this;
//        }
//
//        public CommandCreateStatefulSet build()
//        {
//            return new CommandCreateStatefulSet( this );
//        }
//    }
//}
