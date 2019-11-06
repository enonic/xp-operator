package com.enonic.ec.kubernetes.operator.commands.builders.config;

import java.util.Map;
import java.util.Properties;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.crd.deployment.spec.SpecNode;

public abstract class ConfigBuilder
    extends Configuration
{
    public abstract Map<String, String> create( String nodeResourceName, SpecNode node );

    void apply( SpecNode node, String propertiesName, Properties properties, Map<String, String> config )
    {
        // Always override default values with user input
        node.config().getAsProperties( propertiesName ).ifPresent( p -> p.forEach( properties::put ) );
        config.put( propertiesName, getPropertyAsString( properties ) );
    }

    private static String getPropertyAsString( Properties prop )
    {
        StringBuilder sb = new StringBuilder();
        prop.forEach( ( k, v ) -> sb.append( k.toString() ).append( " = " ).append( v.toString() ).append( "\n" ) );
        return sb.toString();
    }
}
