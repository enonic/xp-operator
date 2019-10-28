package com.enonic.ec.kubernetes.operator.commands.builders.config;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.deployment.xpdeployment.XpDeploymentResourceSpecNode;

public abstract class ConfigBuilder
    extends Configuration
{
    public abstract Map<String, String> create( XpDeploymentResourceSpecNode node );

    protected void apply( XpDeploymentResourceSpecNode node, String propertiesName, Properties properties, Map<String, String> config )
    {
        // Always override default values with user input
        node.configAsProperties( propertiesName ).ifPresent(
            p -> p.entrySet().stream().forEach( e -> properties.put( e.getKey(), e.getValue() ) ) );
        config.put( propertiesName, getPropertyAsString( properties ) );
    }

    private static String getPropertyAsString( Properties prop )
    {
        StringWriter writer = new StringWriter();
        prop.list( new PrintWriter( writer ) );
        return writer.getBuffer().toString();
    }
}
