package com.enonic.cloud.operator.v1alpha2xp7deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import io.fabric8.kubernetes.api.model.ServiceAccount;

import com.enonic.cloud.helm.functions.Templator;
import com.enonic.cloud.helm.values.BaseValues;
import com.enonic.cloud.helm.values.MapValues;
import com.enonic.cloud.helm.values.ValueBuilder;
import com.enonic.cloud.helm.values.Values;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpec;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroupEnvVar;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodesPreinstalledApps;
import com.enonic.cloud.operator.helpers.HandlerHelm;
import com.enonic.cloud.operator.ingress.OperatorXp7ConfigSync;

import static com.enonic.cloud.common.Configuration.cfgFloat;
import static com.enonic.cloud.common.Configuration.cfgInt;
import static com.enonic.cloud.common.Utils.createOwnerReference;
import static com.enonic.cloud.kubernetes.client.Utils.cloneResource;


/**
 * This operator class creates/updates/deletes resources defined in the xp7deployment helm chart
 */
@Singleton
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

    @SuppressWarnings("unchecked")
    @Override
    protected Xp7DeploymentSpec getSpec( final Xp7Deployment t )
    {
        return t.getXp7DeploymentSpec();
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

            if ( !values.containsKey( "settings" ) )
            {
                values.put( "settings", new HashMap<>() );
            }

            ServiceAccount sa = cloudApiSa.get();
            if ( sa != null )
            {
                ( (Map<String, Object>) values.get( "settings" ) ).
                    put( "cloudApiServiceAccount",
                         Map.of( "name", sa.getMetadata().getName(), "namespace", sa.getMetadata().getNamespace() ) );
            }

            for ( Xp7DeploymentSpecNodeGroup ng : resource.getXp7DeploymentSpec().getXp7DeploymentSpecNodeGroups() )
            {
                Optional<Xp7DeploymentSpecNodeGroupEnvVar> optionalXpOpts =
                    ng.getXp7DeploymentSpecNodeGroupEnvironment().stream().filter( e -> e.getName().equals( "XP_OPTS" ) ).findFirst();

                Xp7DeploymentSpecNodeGroupEnvVar xpOpts;
                if ( optionalXpOpts.isEmpty() )
                {
                    xpOpts = new Xp7DeploymentSpecNodeGroupEnvVar().
                        withName( "XP_OPTS" ).
                        withValue( "" );
                    ng.getXp7DeploymentSpecNodeGroupEnvironment().add( xpOpts );
                }
                else
                {
                    xpOpts = optionalXpOpts.get();
                }

                String opts = xpOpts.getValue();

                if ( !( opts.contains( "-Xms" ) || opts.contains( "-Xmx" ) ) )
                {
                    opts = opts + getMemoryOpts( ng );
                }

                if ( !( opts.contains( "-XX:-HeapDumpOnOutOfMemoryError" ) || opts.contains( "-XX:HeapDumpPath" ) ) )
                {
                    opts = opts + " -XX:-HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/enonic-xp/home/data/oom.hprof";
                }

                xpOpts.setValue( opts );
            }

            boolean isClustered = isClustered( resource );
            String pass = suPassProvider.get();

            Map<String, Object> deployment = new HashMap<>();
            deployment.put( "name", resource.getMetadata().getName() );
            deployment.put( "namespace", resource.getMetadata().getNamespace() );
            deployment.put( "clustered", isClustered );
            deployment.put( "suPass", pass );
            deployment.put( "suPassHash", sha512( pass ) );
            deployment.put( "preInstalledAppHash", sha512( resource.
                getXp7DeploymentSpec().
                getNodesPreinstalledApps().
                stream().
                map( Xp7DeploymentSpecNodesPreinstalledApps::getUrl ).
                reduce( String::concat ).
                orElse( "" ) ) );

            if ( isClustered )
            {
                deployment.put( "clusterMajority", getClusterMajority( resource.getXp7DeploymentSpec().getXp7DeploymentSpecNodeGroups() ) );
                deployment.put( "minimumMasterNodes",
                                getMinimumMasterNodes( resource.getXp7DeploymentSpec().getXp7DeploymentSpecNodeGroups() ) );
                deployment.put( "minimumDataNodes",
                                getMinimumDataNodes( resource.getXp7DeploymentSpec().getXp7DeploymentSpecNodeGroups() ) );
            }

            deployment.put( "spec", resource.getXp7DeploymentSpec() );

            values.put( "defaultLabels", defaultLabels( resource ) );
            values.put( "deployment", deployment );

            values.put( "ownerReferences", Collections.singletonList( createOwnerReference( resource ) ) );

            return values;
        }

        private int getClusterMajority( final List<Xp7DeploymentSpecNodeGroup> xp7DeploymentSpecNodeGroups )
        {
            return ( xp7DeploymentSpecNodeGroups.stream().
                map( Xp7DeploymentSpecNodeGroup::getReplicas ).
                reduce( 0, Integer::sum ) / 2 ) + 1;
        }

        private int getMinimumMasterNodes( final List<Xp7DeploymentSpecNodeGroup> xp7DeploymentSpecNodeGroups )
        {
            return ( xp7DeploymentSpecNodeGroups.stream().
                filter( Xp7DeploymentSpecNodeGroup::getMaster ).
                map( Xp7DeploymentSpecNodeGroup::getReplicas ).
                reduce( 0, Integer::sum ) / 2 ) + 1;
        }

        private Object getMinimumDataNodes( final List<Xp7DeploymentSpecNodeGroup> xp7DeploymentSpecNodeGroups )
        {
            return ( xp7DeploymentSpecNodeGroups.stream().
                filter( Xp7DeploymentSpecNodeGroup::getData ).
                map( Xp7DeploymentSpecNodeGroup::getReplicas ).
                reduce( 0, Integer::sum ) / 2 ) + 1;
        }

        private String getMemoryOpts( final Xp7DeploymentSpecNodeGroup ng )
        {
            float memoryInMb = getMemory( ng.getXp7DeploymentSpecNodeGroupResources().getMemory() );

            Float heapMemory;
            if ( ng.getData() )
            {
                heapMemory = memoryInMb * cfgFloat( "operator.deployment.xp.heap.data" );
            }
            else
            {
                heapMemory = memoryInMb * cfgFloat( "operator.deployment.xp.heap.other" );
            }
            int heap = Math.min( Math.round( heapMemory ), cfgInt( "operator.deployment.xp.heap.max" ) );

            return String.format( " -Xms%sm -Xmx%sm", heap, heap );
        }

        private float getMemory( final String memory )
        {
            if ( memory.contains( "Gi" ) )
            {
                return Float.parseFloat( memory.replace( "Gi", "" ) ) * 1024F;
            }
            else if ( memory.contains( "Mi" ) )
            {
                return Float.parseFloat( memory.replace( "Mi", "" ) );
            }
            else
            {
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
            return resource.getXp7DeploymentSpec().
                getXp7DeploymentSpecNodeGroups().
                stream().
                mapToInt( Xp7DeploymentSpecNodeGroup::getReplicas ).sum() > 1;
        }
    }
}
