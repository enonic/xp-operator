package com.enonic.ec.kubernetes.crd.deployment;

import io.fabric8.kubernetes.client.CustomResource;

import com.enonic.ec.kubernetes.crd.deployment.spec.Spec;

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

//    @JsonIgnore
//    public String getEcCloud()
//    {
//        return getLabelWithName( "ec-cloud" );
//    }
//
//    @JsonIgnore
//    public String getEcProject()
//    {
//        return getLabelWithName( "ec-project" );
//    }
//
//    @JsonIgnore
//    public String getEcType()
//    {
//        return getLabelWithName( "ec-type" );
//    }
//
//    @JsonIgnore
//    public String getEcName()
//    {
//        return getLabelWithName( "ec-name" );
//    }
//
//    private String getLabelWithName( String key )
//    {
//        if ( getMetadata().getLabels() == null )
//        {
//            return null;
//        }
//        return getMetadata().getLabels().get( key );
//    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( super.equals( obj ) || spec != null )
        {
            return spec.equals( ( (XpDeploymentResource) obj ).spec );
        }
        return true;
    }
}
