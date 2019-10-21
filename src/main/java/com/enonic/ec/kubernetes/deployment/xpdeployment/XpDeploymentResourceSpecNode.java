package com.enonic.ec.kubernetes.deployment.xpdeployment;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableXpDeploymentResourceSpecNode.Builder.class)
@Value.Immutable
public abstract class XpDeploymentResourceSpecNode
{
    private final static Logger log = LoggerFactory.getLogger( XpDeploymentResourceSpecNode.class );

    public abstract String alias();

    public abstract Integer replicas();

    public abstract NodeType type();

    public abstract XpDeploymentResourceSpecNodeResources resources();

    @Nullable
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
    public Properties configAsProperties( String key )
    {
        String file = config().get( key );
        if ( file == null )
        {
            return null;
        }

        Properties p = new Properties();
        try
        {
            p.load( new StringReader( file ) );
            return p;
        }
        catch ( IOException ex )
        {
            log.warn( "Failed to parse config file: " + key, ex );
            return null;
        }
    }

    @Override
    public String toString()
    {
        return alias();
    }
}
