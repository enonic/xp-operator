package com.enonic.ec.kubernetes.operator.operators.common;

class Xp7DeploymentNotFound
    extends RuntimeException
{
    public Xp7DeploymentNotFound( final String name )
    {
        super( "Xp7Deployment '" + name + "' not found" );
    }
}
