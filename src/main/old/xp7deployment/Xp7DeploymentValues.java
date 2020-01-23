package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment;

import java.util.HashMap;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.info.ValueBuilder;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.InfoXp7Deployment;

@Value.Immutable
public abstract class Xp7DeploymentValues
    extends ValueBuilder<InfoXp7Deployment>
{
    protected abstract String imageTemplate();

    @Override
    protected void createValues( final Map<String, Object> values )
    {
        Map<String, Object> deployment = new HashMap<>();
        deployment.put( "name", info().deploymentName() );
        deployment.put( "clustered", false ); // TODO: Fix
        deployment.put( "spec", info().resource().getSpec() );

        values.put( "image", String.format( imageTemplate(), info().resource().getSpec().xpVersion() ) );
        values.put( "defaultLabels", info().defaultLabels() );
        values.put( "deployment", deployment );
    }
}
