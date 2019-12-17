package com.enonic.ec.kubernetes.operator.crd.deployment;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.fabric8.kubernetes.client.CustomResource;

import com.enonic.ec.kubernetes.operator.crd.deployment.spec.Spec;

import static com.enonic.ec.kubernetes.common.Configuration.cfgStr;

public class XpDeploymentResource
    extends CustomResource
{
    private Spec spec;

    public Spec getSpec()
    {
        return spec;
    }

    public void setSpec( Spec spec )
    {
        this.spec = spec;
    }

    @JsonIgnore
    public String ecCloud()
    {
        return getLabel( cfgStr( "operator.deployment.xp.labels.ec.cloud" ) );
    }

    @JsonIgnore
    public String ecProject()
    {
        return getLabel( cfgStr( "operator.deployment.xp.labels.ec.project" ) );
    }

    @JsonIgnore
    public String ecName()
    {
        return getLabel( cfgStr( "operator.deployment.xp.labels.ec.name" ) );
    }

    private String getLabel( String key )
    {
        if ( this.getMetadata() != null && this.getMetadata().getLabels() != null )
        {
            return this.getMetadata().getLabels().get( key );
        }
        return null;
    }

    @Override
    public boolean equals( final Object obj )
    {
        return super.equals( obj ) && Objects.equals( spec, ( (XpDeploymentResource) obj ).spec );
    }
}
