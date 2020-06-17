package com.enonic.cloud.operator.v1alpha2xp7deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.immutables.value.Value;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import io.fabric8.kubernetes.api.model.ServiceAccount;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.helm.values.BaseValues;
import com.enonic.cloud.helm.values.MapValues;
import com.enonic.cloud.helm.values.ValueBuilder;
import com.enonic.cloud.helm.values.Values;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7Deployment;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroup;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentSpecNodeGroupEnvVar;
import com.enonic.cloud.operator.functions.CreateOwnerReference;

@Value.Immutable
@Params
public abstract class DeploymentHelmValueBuilder
    implements ValueBuilder<Xp7Deployment>
{
    protected abstract BaseValues baseValues();

    protected abstract Supplier<String> suPassProvider();

    protected abstract Supplier<ServiceAccount> cloudApiServiceAccountProvider();

    @Override
    public Values apply( final Xp7Deployment resource )
    {
        MapValues values = new MapValues( baseValues() );

        for ( Xp7DeploymentSpecNodeGroup ng : resource.getXp7DeploymentSpec().getXp7DeploymentSpecNodeGroups() )
        {
            if ( ng.getXp7DeploymentSpecNodeGroupEnvironment().stream().filter(
                e -> e.getName().equals( "JAVA_OPTS" ) ).findFirst().isEmpty() )
            {
                int maxRamPercent = ng.getMaster() ? 75 : 50;

                ng.getXp7DeploymentSpecNodeGroupEnvironment().add( new Xp7DeploymentSpecNodeGroupEnvVar().
                    withName( "JAVA_OPTS" ).
                    withValue(
                        "-Djava.security.properties=/enonic-xp/home/extra-config/java.security.properties -XX:+UseContainerSupport -XX:MinRAMPercentage=20 -XX:InitialRAMPercentage=30 -XX:MaxRAMPercentage=" +
                            maxRamPercent +
                            " -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=60 -XX:+ScavengeBeforeFullGC -XX:+CMSScavengeBeforeRemark -XX:-HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/enonic-xp/home/data/oom.hprof" ) );
            }
        }

        boolean isClustered = isClustered( resource );
        String pass = suPassProvider().get();

        Map<String, Object> deployment = new HashMap<>();
        deployment.put( "name", resource.getMetadata().getName() );
        deployment.put( "namespace", resource.getMetadata().getNamespace() );
        deployment.put( "clustered", isClustered );
        deployment.put( "suPass", pass );
        deployment.put( "suPassHash", sha512( pass ) );
        if ( isClustered )
        {
            deployment.put( "minimumMasterNodes", minimumMasterNodes( resource ) );
            deployment.put( "minimumDataNodes", minimumDataNodes( resource ) );
        }

        deployment.put( "spec", resource.getXp7DeploymentSpec() );

        values.put( "defaultLabels", defaultLabels( resource ) );
        values.put( "deployment", deployment );

        ServiceAccount sa = cloudApiServiceAccountProvider().get();
        if ( sa != null )
        {
            Map<String, Object> cloudApiSa = new HashMap<>();
            cloudApiSa.put( "namespace", sa.getMetadata().getNamespace() );
            cloudApiSa.put( "name", sa.getMetadata().getName() );
            values.put( "cloudApiSa", cloudApiSa );
        }

        values.put( "ownerReferences", Collections.singletonList( new CreateOwnerReference().apply( resource ) ) );

        return values;
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

    private Integer minimumMasterNodes( Xp7Deployment resource )
    {
        return defaultMinimumAvailable( 3 );
    }

    private Integer minimumDataNodes( Xp7Deployment resource )
    {
        return 2;
    }

    private Integer defaultMinimumAvailable( int total )
    {
        if ( total < 2 )
        {
            return 0;
        }
        return ( total / 2 ) + 1;
    }
}
