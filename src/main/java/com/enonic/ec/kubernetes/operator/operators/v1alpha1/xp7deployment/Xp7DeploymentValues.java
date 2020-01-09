package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment;

import java.util.HashMap;
import java.util.Map;

import com.enonic.ec.kubernetes.operator.helm.ValuesBuilder;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.InfoXp7Deployment;

public class Xp7DeploymentValues
    extends ValuesBuilder
{
    private String imageTemplate;

    private InfoXp7Deployment info;

    public Xp7DeploymentValues( final String imageTemplate, final InfoXp7Deployment info )
    {
        super();
        this.imageTemplate = imageTemplate;
        this.info = info;
    }

    @Override
    public Object build()
    {
        Map<String, Object> deployment = new HashMap<>();
        deployment.put( "name", info.deploymentName() );
        deployment.put( "clustered", false ); // TODO: Fix
        deployment.put( "spec", info.resource().getSpec() );

        add( "image", String.format( imageTemplate, info.resource().getSpec().xpVersion() ) );
        add( "defaultLabels", info.defaultLabels() );
        add( "deployment", deployment );

        return map;
    }
}
