package com.enonic.ec.kubernetes.operator.operators.xp7deployment.commands.config;

import java.util.Collections;
import java.util.List;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.info.xp7deployment.InfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.crd.xp7deployment.spec.Xp7DeploymentSpecNode;

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
