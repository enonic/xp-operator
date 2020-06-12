package com.enonic.cloud.operator.v1alpha2xp7deployment;

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
            getAdditionalProperties().values().stream().
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
