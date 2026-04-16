package com.enonic.kubernetes.operator.xp7deployment;

import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.kubernetes.client.v1.xp7deployment.Xp7DeploymentSpecNodeGroupEnvVar;
import com.enonic.kubernetes.helm.functions.Templator;
import com.enonic.kubernetes.helm.values.BaseValues;
import com.enonic.kubernetes.helm.values.MapValues;
import com.enonic.kubernetes.helm.values.ValueBuilder;
import com.enonic.kubernetes.helm.values.Values;
import com.enonic.kubernetes.kubernetes.Clients;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.kubernetes.commands.K8sLogHelper;
import com.enonic.kubernetes.operator.helpers.HandlerHelm;
import com.enonic.kubernetes.operator.ingress.OperatorXp7ConfigSync;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.enonic.kubernetes.common.Configuration.*;
import static com.enonic.kubernetes.common.Utils.createOwnerReference;

/**
 * This operator class creates/updates/deletes resources defined in the xp7deployment helm chart
 */
@ApplicationScoped
public class OperatorXp7DeploymentHelm
    extends HandlerHelm<Xp7Deployment>
{
    @Inject
    @Named("v1/xp7deployment")
    Templator templator;

    @Inject
    OperatorXp7ConfigSync operatorXp7ConfigSync;

    @Inject
    @Named("suPass")
    Supplier<String> suPassSupplier;

    @Inject
    @Named("cloudApi")
    Supplier<ServiceAccount> cloudApi;

    @Inject
    Informers informers;

    @Inject
    Clients clients;

    @ConfigProperty(name = "operator.operator.name")
    String operatorName;

    @ConfigProperty(name = "operator.operator.namespace")
    String operatorNamespace;

    @ConfigProperty(name = "operator.operator.hazelcastClusterRoleName")
    String hazelcastClusterRoleName;

    void onStart( @Observes StartupEvent ev )
    {
        listen( informers.xp7DeploymentInformer() );
    }

    @Override
    public void onNewAdd( final Xp7Deployment newResource )
    {
        ensureNamespaceReady( newResource.getMetadata().getNamespace() );
        super.onNewAdd( newResource );
    }

    @Override
    public void onUpdate( final Xp7Deployment oldResource, final Xp7Deployment newResource )
    {
        ensureNamespaceReady( newResource.getMetadata().getNamespace() );
        super.onUpdate( oldResource, newResource );
    }

    private void ensureNamespaceReady( final String namespace )
    {
        final String managedLabelKey = cfgStr( "operator.charts.values.labelKeys.managed" );

        // 1. Check namespace has managed label
        Namespace ns = clients.k8s().namespaces().withName( namespace ).get();
        if (!"true".equals( ns.getMetadata().getLabels() == null ? null : ns.getMetadata().getLabels().get( managedLabelKey ) )) {
            throw new IllegalStateException( String.format(
                "Namespace '%s' is missing label '%s=true'. " +
                "A cluster admin must label it manually: kubectl label namespace %s %s=true",
                namespace, managedLabelKey, namespace, managedLabelKey ) );
        }

        // 2. Create RoleBinding so the operator SA has permissions in this namespace
        //    before the inner helm chart runs. References the pre-created ClusterRole.
        String clusterRoleName = operatorName + "-namespace-role";
        String rbName = operatorName + "-namespace-access";
        if (clients.k8s().rbac().roleBindings().inNamespace( namespace ).withName( rbName ).get() == null) {
            clients.k8s().rbac().roleBindings().inNamespace( namespace ).resource(
                new RoleBindingBuilder()
                    .withNewMetadata().withName( rbName ).withNamespace( namespace ).endMetadata()
                    .withNewRoleRef()
                        .withApiGroup( "rbac.authorization.k8s.io" )
                        .withKind( "ClusterRole" )
                        .withName( clusterRoleName )
                    .endRoleRef()
                    .addNewSubject()
                        .withKind( "ServiceAccount" )
                        .withName( operatorName )
                        .withNamespace( operatorNamespace )
                    .endSubject()
                    .build()
            ).create();
        }
    }

    @Override
    protected ValueBuilder<Xp7Deployment> getValueBuilder( final BaseValues baseValues )
    {
        return new Xp7DeploymentValueBuilder( baseValues, suPassSupplier, cloudApi, operatorName, hazelcastClusterRoleName );
    }

    @Override
    protected Templator getTemplator()
    {
        return templator;
    }

    @Override
    protected void postHandle( final String namespace )
    {
        // Update all config in namespace after update
        operatorXp7ConfigSync.handle( namespace );
    }

    public static class Xp7DeploymentValueBuilder
        implements ValueBuilder<Xp7Deployment>
    {
        private static final ObjectMapper mapper = new ObjectMapper();

        private final BaseValues baseValues;

        private final Supplier<String> passSupplier;

        private final Supplier<ServiceAccount> cloudApiSa;

        private final String operatorName;

        private final String hazelcastClusterRoleName;

        public Xp7DeploymentValueBuilder( final BaseValues baseValues, final Supplier<String> passSupplier,
                                          final Supplier<ServiceAccount> cloudApiSa, final String operatorName,
                                          final String hazelcastClusterRoleName )
        {
            this.baseValues = baseValues;
            this.passSupplier = passSupplier;
            this.cloudApiSa = cloudApiSa;
            this.operatorName = operatorName;
            this.hazelcastClusterRoleName = hazelcastClusterRoleName;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Values apply( final Xp7Deployment in )
        {
            Xp7Deployment resource = cloneResource( in );
            MapValues values = new MapValues( baseValues );

            if (!values.containsKey( "settings" )) {
                values.put( "settings", new HashMap<>() );
            }

            ServiceAccount sa = cloudApiSa.get();
            if (sa != null) {
                ((Map<String, Object>) values.get( "settings" )).
                    put( "cloudApiServiceAccount",
                        Map.of( "name", sa.getMetadata().getName(), "namespace", sa.getMetadata().getNamespace() ) );
            }

            for (Xp7DeploymentSpecNodeGroup ng : resource.getSpec().getXp7DeploymentSpecNodeGroups()) {
                Optional<Xp7DeploymentSpecNodeGroupEnvVar> optionalXpOpts =
                    ng.getXp7DeploymentSpecNodeGroupEnvironment().stream().filter( e -> e.getName().equals( "XP_OPTS" ) ).findFirst();

                Xp7DeploymentSpecNodeGroupEnvVar xpOpts;
                if (optionalXpOpts.isEmpty()) {
                    xpOpts = new Xp7DeploymentSpecNodeGroupEnvVar().
                        withName( "XP_OPTS" ).
                        withValue( "" );
                    ng.getXp7DeploymentSpecNodeGroupEnvironment().add( xpOpts );
                } else {
                    xpOpts = optionalXpOpts.get();
                }

                String opts = xpOpts.getValue();

                if ( !( opts.contains( "-Xms" ) || opts.contains( "-Xmx" ) ) )
                {
                    opts = joinOpts( opts, getMemoryOpts( ng ) );
                }

                if ( !( opts.contains( "-XX:-HeapDumpOnOutOfMemoryError" ) || opts.contains( "-XX:HeapDumpPath" ) ) )
                {
                    opts = joinOpts( opts, "-XX:-HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/enonic-xp/home/data/oom.hprof" );
                }

                if ( !( opts.contains( "-Dhazelcast.shutdownhook.policy" ) ) )
                {
                    opts = joinOpts( opts, "-Dhazelcast.shutdownhook.policy=GRACEFUL" );
                }

                if (!(opts.contains( "-Dhazelcast.graceful.shutdown.max.wait" ))) {
                    opts = joinOpts( opts, "-Dhazelcast.graceful.shutdown.max.wait=" +
                        cfgStr( "operator.charts.values.pods.terminationGracePeriodSeconds" ) );
                }

                xpOpts.setValue( opts );
            }

            boolean isClustered = isClustered( resource );
            String pass = passSupplier.get();

            Map<String, Object> deployment = new HashMap<>();
            deployment.put( "name", resource.getMetadata().getName() );
            deployment.put( "namespace", resource.getMetadata().getNamespace() );
            deployment.put( "clustered", isClustered );
            deployment.put( "hasDedicatedFrontendNodes", hasDedicatedFrontendNodes( resource ) );
            deployment.put( "suPass", pass );
            deployment.put( "suPassHash", sha512( pass ) );

            if (isClustered) {
                deployment.put( "clusterMajority", getClusterMajority( resource.getSpec().getXp7DeploymentSpecNodeGroups() ) );
                deployment.put( "minimumMasterNodes", getMinimumMasterNodes( resource.getSpec().getXp7DeploymentSpecNodeGroups() ) );
                deployment.put( "minimumDataNodes", getMinimumDataNodes( resource.getSpec().getXp7DeploymentSpecNodeGroups() ) );
            }

            deployment.put( "spec", resource.getSpec() );

            values.put( "defaultLabels", defaultLabels( resource ) );

            final Map<String, Object> metadata = new HashMap<>();

            final Map<String, String> annotations = annatations( resource );
            if(!annotations.isEmpty())
            {
                metadata.put( "annotations", annotations );
                deployment.put( "metadata", metadata );
            }

            values.put( "deployment", deployment );

            values.put( "ownerReferences", Collections.singletonList( createOwnerReference( resource ) ) );

            // Operator identity values consumed by inner helm templates (e.g. base_sa.yaml)
            Map<String, Object> operatorValues = new HashMap<>();
            operatorValues.put( "name", operatorName );
            operatorValues.put( "hazelcastClusterRoleName", hazelcastClusterRoleName );
            values.put( "operator", operatorValues );

            return values;
        }

        private boolean hasDedicatedFrontendNodes( Xp7Deployment deployment )
        {
            return deployment.getSpec().getXp7DeploymentSpecNodeGroups().stream()
                .filter( n -> !n.getData() )
                .anyMatch( n -> !n.getMaster() );
        }

        private Xp7Deployment cloneResource( final Xp7Deployment in )
        {
            try {
                return mapper.readValue( mapper.writeValueAsString( in ), Xp7Deployment.class );
            } catch (JsonProcessingException e) {
                throw new RuntimeException( e );
            }
        }

        private int getClusterMajority( final List<Xp7DeploymentSpecNodeGroup> xp7DeploymentSpecNodeGroups )
        {
            return (xp7DeploymentSpecNodeGroups.stream().
                map( Xp7DeploymentSpecNodeGroup::getReplicas ).
                reduce( 0, Integer::sum ) / 2) + 1;
        }

        private int getMinimumMasterNodes( final List<Xp7DeploymentSpecNodeGroup> xp7DeploymentSpecNodeGroups )
        {
            return (xp7DeploymentSpecNodeGroups.stream().
                filter( Xp7DeploymentSpecNodeGroup::getMaster ).
                map( Xp7DeploymentSpecNodeGroup::getReplicas ).
                reduce( 0, Integer::sum ) / 2) + 1;
        }

        private Object getMinimumDataNodes( final List<Xp7DeploymentSpecNodeGroup> xp7DeploymentSpecNodeGroups )
        {
            return (xp7DeploymentSpecNodeGroups.stream().
                filter( Xp7DeploymentSpecNodeGroup::getData ).
                map( Xp7DeploymentSpecNodeGroup::getReplicas ).
                reduce( 0, Integer::sum ) / 2) + 1;
        }

        private String getMemoryOpts( final Xp7DeploymentSpecNodeGroup ng )
        {
            double memoryInMb = getMemory( ng.getXp7DeploymentSpecNodeGroupResources().getMemory() );

            double heapMemory;
            if (ng.getData()) {
                heapMemory = memoryInMb * cfgFloat( "operator.deployment.xp.heap.data" );
            } else {
                heapMemory = memoryInMb * cfgFloat( "operator.deployment.xp.heap.other" );
            }
            int heap = Math.toIntExact( Math.min( Math.round( heapMemory ), cfgInt( "operator.deployment.xp.heap.max" ) ) );

            return String.format( "-Xms%sm -Xmx%sm", heap, heap );
        }

        private double getMemory( final String memory )
        {
            if (memory.contains( "Gi" )) {
                return Double.parseDouble( memory.replace( "Gi", "" ) ) * 1024;
            } else if (memory.contains( "Mi" )) {
                return Double.parseDouble( memory.replace( "Mi", "" ) );
            } else {
                throw new RuntimeException( "Invalid memory mappings" );
            }
        }

        @SuppressWarnings("UnstableApiUsage")
        private Object sha512( final String s )
        {
            return Hashing.sha512().hashString( s, Charsets.UTF_8 ).toString();
        }

        private Map<String, String> defaultLabels( final Xp7Deployment resource )
        {
            return resource.getMetadata().getLabels();
        }

        private Map<String, String> annatations( final Xp7Deployment resource )
        {
            return resource.getMetadata().getAnnotations();
        }

        private boolean isClustered( Xp7Deployment resource )
        {
            return resource.getSpec().
                getXp7DeploymentSpecNodeGroups().
                stream().
                mapToInt( Xp7DeploymentSpecNodeGroup::getReplicas ).sum() > 1;
        }

        private static String joinOpts( String... opts )
        {
            return Stream.of( opts )
                .filter( Objects::nonNull )
                .filter( Predicate.not( String::isEmpty ) )
                .collect( Collectors.joining( " " ) );
        }
    }
}
