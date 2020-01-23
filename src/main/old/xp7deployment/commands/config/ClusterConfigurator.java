package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.commands.config;

import java.util.List;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.kubectl.apply.ImmutableCommandApplyXp7Config;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client.Xp7ConfigClient;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.spec.ImmutableXp7ConfigSpec;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.spec.Xp7DeploymentSpecNode;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.InfoXp7Deployment;

public abstract class ClusterConfigurator
    extends Configuration
{
    public abstract Xp7ConfigClient client();

    protected abstract InfoXp7Deployment info();

    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder, String nodeId, Xp7DeploymentSpecNode node )
    {
        StringBuilder elasticSearchConfig = new StringBuilder();
        setElasticSearchConfig( elasticSearchConfig, node );

        commandBuilder.addCommand( ImmutableCommandApplyXp7Config.builder().
            client( client() ).
            canSkipOwnerReference( true ).
            namespace( info().namespaceName() ).
            name( cfgStrFmt( "operator.config.xp.elasticsearch.name", nodeId ) ).
            spec( ImmutableXp7ConfigSpec.builder().
                file( cfgStr( "operator.config.xp.elasticsearch.file" ) ).
                data( elasticSearchConfig.toString() ).
                node( nodeId ).
                build() ).
            build() );

        StringBuilder clusterConfig = new StringBuilder();
        setClusterConfig( clusterConfig );

        commandBuilder.addCommand( ImmutableCommandApplyXp7Config.builder().
            client( client() ).
            canSkipOwnerReference( true ).
            namespace( info().namespaceName() ).
            name( cfgStrFmt( "operator.config.xp.cluster.name", nodeId ) ).
            spec( ImmutableXp7ConfigSpec.builder().
                file( cfgStr( "operator.config.xp.cluster.file" ) ).
                data( clusterConfig.toString() ).
                node( nodeId ).
                build() ).
            build() );
    }

    @Value.Derived
    public List<String> waitForDnsRecords()
    {
        return createWaitForDnsRecordsList( info() );
    }

    protected abstract List<String> createWaitForDnsRecordsList( InfoXp7Deployment info );

    protected abstract void setElasticSearchConfig( final StringBuilder sb, Xp7DeploymentSpecNode node );

    protected abstract void setClusterConfig( final StringBuilder sb );
}
