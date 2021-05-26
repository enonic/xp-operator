package com.enonic.kubernetes.operator.v1alpha2xp7deployment;

import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroupEnvVar;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpecNodesPreinstalledApps;
import com.enonic.kubernetes.helm.functions.Templator;
import com.enonic.kubernetes.helm.values.BaseValues;
import com.enonic.kubernetes.helm.values.MapValues;
import com.enonic.kubernetes.helm.values.ValueBuilder;
import com.enonic.kubernetes.helm.values.Values;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.operator.helpers.HandlerHelm;
import com.enonic.kubernetes.operator.ingress.OperatorXp7ConfigSync;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.enonic.kubernetes.common.Configuration.cfgFloat;
import static com.enonic.kubernetes.common.Configuration.cfgInt;
import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.common.Utils.createOwnerReference;


/**
 * This operator class creates/updates/deletes resources defined in the xp7deployment helm chart
 */
@ApplicationScoped
public class OperatorXp7DeploymentHelm
    extends HandlerHelm<Xp7Deployment>
{
    @Inject
    @Named("v1alpha2/xp7deployment")
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

    void onStart( @Observes StartupEvent ev )
    {
        listen( informers.xp7DeploymentInformer() );
    }

    @Override
    protected ValueBuilder<Xp7Deployment> getValueBuilder( final BaseValues baseValues )
    {
        return new Xp7DeploymentValueBuilder( baseValues, suPassSupplier, cloudApi );
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

        private final Supplier<String> suPassProvider;

        private final Supplier<ServiceAccount> cloudApiSa;

        public Xp7DeploymentValueBuilder( final BaseValues baseValues, final Supplier<String> suPassProvider,
                                          final Supplier<ServiceAccount> cloudApiSa )
        {
            this.baseValues = baseValues;
            this.suPassProvider = suPassProvider;
            this.cloudApiSa = cloudApiSa;
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

                if (!(opts.contains( "-Xms" ) || opts.contains( "-Xmx" ))) {
                    opts = opts + getMemoryOpts( ng );
                }

                if (!(opts.contains( "-XX:-HeapDumpOnOutOfMemoryError" ) || opts.contains( "-XX:HeapDumpPath" ))) {
                    opts = opts + " -XX:-HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/enonic-xp/home/data/oom.hprof";
                }

                if (!(opts.contains( "-Dhazelcast.shutdownhook.policy" ))) {
                    opts = opts + " -Dhazelcast.shutdownhook.policy=GRACEFUL";
                }

                if (!(opts.contains( "-Dhazelcast.graceful.shutdown.max.wait" ))) {
                    opts = opts + " -Dhazelcast.graceful.shutdown.max.wait=" + cfgStr( "operator.charts.values.pods.terminationGracePeriodSeconds" );
                }

                xpOpts.setValue( opts );
            }

            boolean isClustered = isClustered( resource );
            String pass = suPassProvider.get();

            Map<String, Object> deployment = new HashMap<>();
            deployment.put( "name", resource.getMetadata().getName() );
            deployment.put( "namespace", resource.getMetadata().getNamespace() );
            deployment.put( "clustered", isClustered );
            deployment.put( "hasDedicatedFrontendNodes", hasDedicatedFrontendNodes( resource ) );
            deployment.put( "suPass", pass );
            deployment.put( "suPassHash", sha512( pass ) );
            deployment.put( "preInstalledAppHash", sha512( resource.
                getSpec().
                getNodesPreinstalledApps().
                stream().
                map( Xp7DeploymentSpecNodesPreinstalledApps::getUrl ).
                reduce( String::concat ).
                orElse( "" ) ) );

            if (isClustered) {
                deployment.put( "clusterMajority", getClusterMajority( resource.getSpec().getXp7DeploymentSpecNodeGroups() ) );
                deployment.put( "minimumMasterNodes", getMinimumMasterNodes( resource.getSpec().getXp7DeploymentSpecNodeGroups() ) );
                deployment.put( "minimumDataNodes", getMinimumDataNodes( resource.getSpec().getXp7DeploymentSpecNodeGroups() ) );
            }

            deployment.put( "spec", resource.getSpec() );

            values.put( "defaultLabels", defaultLabels( resource ) );
            values.put( "deployment", deployment );

            values.put( "ownerReferences", Collections.singletonList( createOwnerReference( resource ) ) );

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
            float memoryInMb = getMemory( ng.getXp7DeploymentSpecNodeGroupResources().getMemory() );

            Float heapMemory;
            if (ng.getData()) {
                heapMemory = memoryInMb * cfgFloat( "operator.deployment.xp.heap.data" );
            } else {
                heapMemory = memoryInMb * cfgFloat( "operator.deployment.xp.heap.other" );
            }
            int heap = Math.min( Math.round( heapMemory ), cfgInt( "operator.deployment.xp.heap.max" ) );

            return String.format( " -Xms%sm -Xmx%sm", heap, heap );
        }

        private float getMemory( final String memory )
        {
            if (memory.contains( "Gi" )) {
                return Float.parseFloat( memory.replace( "Gi", "" ) ) * 1024F;
            } else if (memory.contains( "Mi" )) {
                return Float.parseFloat( memory.replace( "Mi", "" ) );
            } else {
                throw new RuntimeException( "Invalid memory mappings" );
            }
        }

        @SuppressWarnings("UnstableApiUsage")
        private Object sha512( final String s )
        {
            return Hashing.sha512().hashString( s, Charsets.UTF_8 ).toString();
        }

        private Object defaultLabels( final Xp7Deployment resource )
        {
            return resource.getMetadata().getLabels();
        }

        private boolean isClustered( Xp7Deployment resource )
        {
            return resource.getSpec().
                getXp7DeploymentSpecNodeGroups().
                stream().
                mapToInt( Xp7DeploymentSpecNodeGroup::getReplicas ).sum() > 1;
        }
    }
}
