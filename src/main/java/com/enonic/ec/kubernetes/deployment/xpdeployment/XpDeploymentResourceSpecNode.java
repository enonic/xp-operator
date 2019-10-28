package com.enonic.ec.kubernetes.deployment.xpdeployment;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.common.Configuration;

@JsonDeserialize(builder = ImmutableXpDeploymentResourceSpecNode.Builder.class)
@Value.Immutable
public abstract class XpDeploymentResourceSpecNode
    extends Configuration
{
    private final static Logger log = LoggerFactory.getLogger( XpDeploymentResourceSpecNode.class );

    public abstract String alias();

    public abstract Integer replicas();

    public abstract NodeType type();

    public abstract XpDeploymentResourceSpecNodeResources resources();

    public abstract Map<String, String> env();

    public abstract Map<String, String> config();

    @JsonIgnore
    @Value.Derived
    public Map<String, String> nodeAliasLabel()
    {
        // Do not add more labels here, it will brake service mapping for vHosts to pods
        return Map.of( cfgStr( "operator.deployment.xp.pod.label.aliasPrefix" ) + alias(), alias() );
    }

    @JsonIgnore
    @Value.Derived
    public Map<String, String> nodeExtraLabels( Map<String, String> defaultLabels )
    {
        Map<String, String> res = new HashMap<>( defaultLabels );
        res.putAll( nodeAliasLabel() );
        switch ( type() )
        {
            case STANDALONE:
                res.put( cfgStr( "operator.deployment.xp.labels.nodeType.master" ), "true" );
                res.put( cfgStr( "operator.deployment.xp.labels.nodeType.data" ), "true" );
                res.put( cfgStr( "operator.deployment.xp.labels.nodeType.frontend" ), "true" );
                break;
            case MASTER:
                res.put( cfgStr( "operator.deployment.xp.labels.nodeType.master" ), "true" );
                break;
            case DATA:
                res.put( cfgStr( "operator.deployment.xp.labels.nodeType.data" ), "true" );
                break;
            case FRONT:
                res.put( cfgStr( "operator.deployment.xp.labels.nodeType.frontend" ), "true" );
                break;
        }
        return res;
    }

    @JsonIgnore
    @Value.Derived
    public Integer minimumAvailable()
    {
        // TODO: Reconsider
        Integer min = replicas() / 2 + 1;

        if ( min >= replicas() )
        {
            min = replicas() - 1;
        }

        if ( min != null )
        {
            Preconditions.checkState( min % 2 == 1, "minimum available should be an odd number" );
        }

        return min;
    }

    @JsonIgnore
    @Value.Derived
    public Optional<Properties> configAsProperties( String key )
    {
        Optional<String> file = Optional.ofNullable( config().get( key ) );
        if ( file.isEmpty() )
        {
            return Optional.empty();
        }

        Properties p = new Properties();
        try
        {
            p.load( new StringReader( file.get() ) );
            return Optional.of( p );
        }
        catch ( IOException ex )
        {
            log.warn( "Failed to parse config file: " + key, ex );
            return Optional.empty();
        }
    }

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( resources().disks().containsKey( "index" ), "field resources.disks.index on node has to be set" );
        Preconditions.checkState( resources().disks().containsKey( "snapshots" ), "field resources.disks.snapshots on node has to be set" );

        if ( type() == NodeType.STANDALONE || type() == NodeType.MASTER )
        {
            Preconditions.checkState( replicas() % 2 == 0, "field replicas has to be a odd number on node types: " +
                Arrays.asList( NodeType.MASTER, NodeType.STANDALONE ) );
        }
    }

    @Override
    public String toString()
    {
        return alias();
    }
}
