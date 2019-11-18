package com.enonic.ec.kubernetes.crd.deployment;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.crd.deployment.spec.SpecNode;

@Value.Immutable
public abstract class XpDeploymentNamingHelper
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
        return String.join( "-", resource().getEcType(), resource().getEcName() );
    }

    public String defaultResourceNameWithPostFix( String... postfix )
    {
        return String.join( "-", defaultResourceName(), String.join( "-", postfix ) );
    }

    public String defaultResourceNameWithPreFix( String... prefix )
    {
        return String.join( "-", String.join( "-", prefix ), defaultResourceName() );
    }

    public String defaultResourceName( SpecNode node )
    {
        return defaultResourceNameWithPostFix( node.alias() );
    }
}
