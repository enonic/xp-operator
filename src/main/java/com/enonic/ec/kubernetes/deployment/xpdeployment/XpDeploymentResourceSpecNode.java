package com.enonic.ec.kubernetes.deployment.xpdeployment;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

@JsonDeserialize(builder = ImmutableXpDeploymentResourceSpecNode.Builder.class)
@Value.Immutable
public abstract class XpDeploymentResourceSpecNode
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
        // Do not add more labels here, it will brake service mapping to pods
        return Map.of( "xp.node.alias." + alias(), alias() );
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
        Preconditions.checkState( resources().disks().containsKey( "repo" ), "field resources.disks.repo on node has to be set" );
        Preconditions.checkState( resources().disks().containsKey( "snapshots" ), "field resources.disks.snapshots on node has to be set" );
    }

    @Override
    public String toString()
    {
        return alias();
    }
}
