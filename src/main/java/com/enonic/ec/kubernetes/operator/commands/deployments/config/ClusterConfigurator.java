package com.enonic.ec.kubernetes.operator.commands.deployments.config;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.kubectl.apply.ImmutableCommandApplyXp7Config;
import com.enonic.ec.kubernetes.operator.crd.config.client.XpConfigClient;
import com.enonic.ec.kubernetes.operator.crd.config.spec.ImmutableSpec;
import com.enonic.ec.kubernetes.operator.crd.deployment.spec.SpecNode;

public abstract class ClusterConfigurator
    extends Configuration
{
    public abstract XpConfigClient client();

    public abstract String namespace();

    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder, String nodeId, SpecNode node )
    {
        StringBuilder elasticSearchConfig = new StringBuilder();
        setElasticSearchConfig( elasticSearchConfig, node );

        commandBuilder.addCommand( ImmutableCommandApplyXp7Config.builder().
            client( client() ).
            canSkipOwnerReference( true ).
            namespace( namespace() ).
            name( cfgStrFmt( "operator.config.xp.elasticsearch.name", nodeId ) ).
            spec( ImmutableSpec.builder().
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
            namespace( namespace() ).
            name( cfgStrFmt( "operator.config.xp.cluster.name", nodeId ) ).
            spec( ImmutableSpec.builder().
                file( cfgStr( "operator.config.xp.cluster.file" ) ).
                data( clusterConfig.toString() ).
                node( nodeId ).
                build() ).
            build() );
    }

    protected abstract void setElasticSearchConfig( final StringBuilder sb, SpecNode node );

    protected abstract void setClusterConfig( final StringBuilder sb );
}