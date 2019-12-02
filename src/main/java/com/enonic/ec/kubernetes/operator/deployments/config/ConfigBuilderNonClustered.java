package com.enonic.ec.kubernetes.operator.deployments.config;

import java.util.Map;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.crd.deployment.spec.SpecNode;

@Value.Immutable
public abstract class ConfigBuilderNonClustered
    extends ConfigBuilderNode
{
    @Override
    public Map<String, String> create( SpecNode node )
    {
        Map<String, String> newConfig = overrideWithValues( node.config() );

        // Create cluster config
        setIfNotSet( newConfig, "com.enonic.xp.cluster.cfg", setFunc -> {
            setFunc.apply( "cluster.enabled", "false" );
        } );

        // Create elastic config
        setIfNotSet( newConfig, "com.enonic.xp.elasticsearch.cfg", setFunc -> {
            setFunc.apply( "http.enabled", "true" ); // TODO: Use alive app for health checks
        } );

        return newConfig;
    }
}
