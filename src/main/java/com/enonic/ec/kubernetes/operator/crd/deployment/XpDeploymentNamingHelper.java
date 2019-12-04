package com.enonic.ec.kubernetes.operator.crd.deployment;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.common.Configuration;

@Value.Immutable
public abstract class XpDeploymentNamingHelper
    extends Configuration
{
    protected abstract XpDeploymentResource resource();

    @Value.Derived
    public String defaultNamespaceName()
    {
        return resource().getMetadata().getName();
    }

    @Value.Derived
    public String defaultResourceName()
    {
        String type = resource().getMetadata().getLabels().get( cfgStr( "operator.deployment.xp.labels.ec.type" ) );
        String name = resource().getMetadata().getLabels().get( cfgStr( "operator.deployment.xp.labels.ec.name" ) );
        Preconditions.checkState( type != null, "Type cannot be null" );
        Preconditions.checkState( name != null, "Name cannot be null" );
        return String.join( "-", type, name );
    }

    public String defaultResourceNameWithPostFix( String... postfix )
    {
        return String.join( "-", defaultResourceName(), String.join( "-", postfix ) );
    }

    public String defaultResourceNameWithPreFix( String... prefix )
    {
        return String.join( "-", String.join( "-", prefix ), defaultResourceName() );
    }

    public String defaultResourceName( String name )
    {
        return defaultResourceNameWithPostFix( name );
    }

//    public String defaultResourceName( SpecNode node )
//    {
//        return defaultResourceNameWithPostFix( node.alias() );
//    }
}
