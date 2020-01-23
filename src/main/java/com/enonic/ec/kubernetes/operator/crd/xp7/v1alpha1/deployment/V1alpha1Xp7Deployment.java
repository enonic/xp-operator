package com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.deployment;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.fabric8.kubernetes.client.CustomResource;

import static com.enonic.ec.kubernetes.operator.common.Configuration.cfgStr;

public class V1alpha1Xp7Deployment
    extends CustomResource
{
    private V1alpha1Xp7DeploymentSpec spec;

    public V1alpha1Xp7DeploymentSpec getSpec()
    {
        return spec;
    }

    public void setSpec( V1alpha1Xp7DeploymentSpec spec )
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
        return super.equals( obj ) && Objects.equals( spec, ( (V1alpha1Xp7Deployment) obj ).spec );
    }
}
