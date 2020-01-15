package com.enonic.ec.kubernetes.operator.helm.v1alpha1.xp7deployment;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.enonic.ec.kubernetes.operator.helm.HelmTest;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.ImmutableXp7DeploymentValues;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.ImmutableInfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.InfoXp7Deployment;

public class Xp7DeploymentTest
    extends HelmTest
{
    @Override
    protected String chartToTest()
    {
        return "v1alpha1/xp7deployment";
    }

    @Override
    protected Object createValues( final ObjectMapper mapper, final File input )
        throws IOException
    {
        Xp7DeploymentResource res = mapper.readValue( input, Xp7DeploymentResource.class );
        InfoXp7Deployment info = ImmutableInfoXp7Deployment.builder().
            newResource( res ).
            build();
        return ImmutableXp7DeploymentValues.builder().
            info( info ).
            imageTemplate( "%s" ).
            build().
            values();
    }
}
