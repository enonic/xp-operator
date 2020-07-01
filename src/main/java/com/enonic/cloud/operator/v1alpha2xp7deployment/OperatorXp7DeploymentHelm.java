package com.enonic.cloud.operator.v1alpha2xp7deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroupEnvVar;
import com.enonic.cloud.operator.helpers.HandlerHelm;

import static com.enonic.cloud.common.Configuration.cfgHasKey;
import static com.enonic.cloud.common.Configuration.cfgStr;
import static com.enonic.cloud.common.Utils.createOwnerReference;
import static com.enonic.cloud.kubernetes.client.Utils.cloneResource;


@Singleton
public class OperatorXp7DeploymentHelm
    extends HandlerHelm<Xp7Deployment>
{
    @Inject
    Clients clients;

    @Inject
    @Named("v1alpha2/xp7deployment")
    Templator templator;

    @Override
    protected ValueBuilder<Xp7Deployment> getValueBuilder( final BaseValues baseValues )
    {
        return new Xp7DeploymentValueBuilder( baseValues, this::createSuPass, this::cloudApiSa );
    }

    @Override
    protected Templator getTemplator()
    {
        return templator;
    }

    @Override
    protected boolean specEquals( final Xp7Deployment oldResource, final Xp7Deployment newResource )
    {
        return oldResource.getXp7DeploymentSpec().equals( newResource.getXp7DeploymentSpec() );
    }

    private String createSuPass()
    {
        if ( cfgHasKey( "operator.deployment.fixedSuPass" ) )
        {
            return cfgStr( "operator.deployment.fixedSuPass" );
        }
        else
        {
            return UUID.randomUUID().toString().replace( "-", "" ).toLowerCase();
        }
    }

    private ServiceAccount cloudApiSa()
    {
        return clients.k8s().serviceAccounts().
            inNamespace( cfgStr( "operator.deployment.adminServiceAccount.namespace" ) ).
            withName( cfgStr( "operator.deployment.adminServiceAccount.name" ) ).
            get();
    }

    public static class Xp7DeploymentValueBuilder
        implements ValueBuilder<Xp7Deployment>
    {
        private final BaseValues baseValues;

        private final Supplier<String> suPassProvider;

        private final Supplier<ServiceAccount> cloudApiServiceAccountProvider;

        public Xp7DeploymentValueBuilder( final BaseValues baseValues, final Supplier<String> suPassProvider,
                                          final Supplier<ServiceAccount> cloudApiServiceAccountProvider )
        {
            this.baseValues = baseValues;
            this.suPassProvider = suPassProvider;
            this.cloudApiServiceAccountProvider = cloudApiServiceAccountProvider;
        }

        @Override
        public Values apply( final Xp7Deployment in )
        {
            Xp7Deployment resource = cloneResource( in );
            MapValues values = new MapValues( baseValues );

            for ( Xp7DeploymentSpecNodeGroup ng : resource.getXp7DeploymentSpec().getXp7DeploymentSpecNodeGroups() )
            {
                Optional<Xp7DeploymentSpecNodeGroupEnvVar> optionalXpOpts =
                    ng.getXp7DeploymentSpecNodeGroupEnvironment().stream().filter( e -> e.getName().equals( "XP_OPTS" ) ).findFirst();

                Xp7DeploymentSpecNodeGroupEnvVar xpOpts = null;
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

                if ( !(opts.contains( "-Xms" ) || opts.contains( "-Xmx" )) )
                {
                    opts = opts + getMemoryOpts( ng );
                }

                if ( !(opts.contains( "-XX:-HeapDumpOnOutOfMemoryError" ) || opts.contains( "-XX:HeapDumpPath" ))) {
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
            if ( isClustered )
            {
                deployment.put( "minimumMasterNodes", 2 );
                deployment.put( "minimumDataNodes", 2 );
            }

            deployment.put( "spec", resource.getXp7DeploymentSpec() );

            values.put( "defaultLabels", defaultLabels( resource ) );
            values.put( "deployment", deployment );

            ServiceAccount sa = cloudApiServiceAccountProvider.get();
            if ( sa != null )
            {
                Map<String, Object> cloudApiSa = new HashMap<>();
                cloudApiSa.put( "namespace", sa.getMetadata().getNamespace() );
                cloudApiSa.put( "name", sa.getMetadata().getName() );
                values.put( "cloudApiSa", cloudApiSa );
            }

            values.put( "ownerReferences", Collections.singletonList( createOwnerReference( resource ) ) );

            return values;
        }

        private String getMemoryOpts( final Xp7DeploymentSpecNodeGroup ng )
        {
            float memoryInMb = getMemory( ng.getXp7DeploymentSpecNodeGroupResources().getMemory() );

            Float heapMemory;
            if ( ng.getData() )
            {
                heapMemory = memoryInMb * 0.5f;
            }
            else
            {
                heapMemory = memoryInMb * 0.75f;
            }

            int heap = Math.round( heapMemory );

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