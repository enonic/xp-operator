package com.enonic.ec.kubernetes.operator.commands.deployments.config;

import java.util.Collections;
import java.util.List;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.deployment.diff.InfoDeployment;
import com.enonic.ec.kubernetes.operator.crd.deployment.spec.SpecNode;

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
    protected void setElasticSearchConfig( final StringBuilder sb, SpecNode node )
    {
        sb.append( "http.enabled=" ).append( "false" ).append( "\n" );
    }

    @Override
    protected List<String> createWaitForDnsRecordsList( final InfoDeployment info )
    {
        return Collections.emptyList();
    }
}
