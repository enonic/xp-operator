package com.enonic.ec.kubernetes.operator;

public class XpDeploymentNotFound
    extends RuntimeException
{
    public XpDeploymentNotFound( final String name )
    {
        super( "XpDeployment '" + name + "' not found" );
    }
}
