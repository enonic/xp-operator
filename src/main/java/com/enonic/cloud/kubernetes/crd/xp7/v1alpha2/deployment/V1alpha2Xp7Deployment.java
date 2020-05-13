package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.enonic.cloud.kubernetes.crd.status.HasStatus;

import static com.enonic.cloud.common.Configuration.cfgStr;


public class V1alpha2Xp7Deployment
    extends HasStatus<V1alpha2Xp7DeploymentStatusFields, V1alpha2Xp7DeploymentStatus>
{
    private V1alpha2Xp7DeploymentSpec spec;

    public V1alpha2Xp7DeploymentSpec getSpec()
    {
        return spec;
    }

    @SuppressWarnings("unused")
    public void setSpec( V1alpha2Xp7DeploymentSpec spec )
    {
        this.spec = spec;
    }

    @JsonIgnore
    public String ecCloud()
    {
        return getLabel( cfgStr( "operator.deployment.xp.labels.cloud" ) );
    }

    @JsonIgnore
    public String ecSolution()
    {
        return getLabel( cfgStr( "operator.deployment.xp.labels.solution" ) );
    }

    @JsonIgnore
    public String ecEnvironment()
    {
        return getLabel( cfgStr( "operator.deployment.xp.labels.environment" ) );
    }

    @JsonIgnore
    public String ecService()
    {
        return getLabel( cfgStr( "operator.deployment.xp.labels.service" ) );
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
        return super.equals( obj ) && Objects.equals( spec, ( (V1alpha2Xp7Deployment) obj ).spec );
    }
}
