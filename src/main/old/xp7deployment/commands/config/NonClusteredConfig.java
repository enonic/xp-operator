package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.config;

import java.util.Collections;
import java.util.List;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.Xp7DeploymentSpecNode;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.InfoXp7Deployment;

@Value.Immutable
public abstract class NonClusteredConfig
    extends ClusterConfigurator
{
    @Override
    protected void setClusterConfig( final StringBuilder sb )
    {
        sb.append( "cluster.enabled=" ).append( "false" ).append( "\n" );
    }

    @Override
    protected void setElasticSearchConfig( final StringBuilder sb, Xp7DeploymentSpecNode node )
    {
        sb.append( "http.enabled=" ).append( "false" ).append( "\n" );
    }

    @Override
    protected List<String> createWaitForDnsRecordsList( final InfoXp7Deployment info )
    {
        return Collections.emptyList();
    }
}
