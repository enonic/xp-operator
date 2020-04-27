package com.enonic.cloud.operator.helm.v1alpha2.xp7deployment;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.helm.BaseValues;
import com.enonic.cloud.operator.helm.HelmTest;
import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.ImmutableXp7DeploymentValues;
import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.info.ImmutableInfoXp7Deployment;
import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.info.InfoXp7Deployment;

@SuppressWarnings("WeakerAccess")
public class Xp7DeploymentTest
    extends HelmTest
{
    @Override
    protected String chartToTest()
    {
        return "v1alpha2/xp7deployment";
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    protected Object createValues( final ObjectMapper mapper, final File input )
        throws IOException
    {
        V1alpha2Xp7Deployment resource = mapper.readValue( input, V1alpha2Xp7Deployment.class );
        InfoXp7Deployment info = ImmutableInfoXp7Deployment.builder().
            newResource( resource ).
            build();
        return ImmutableXp7DeploymentValues.builder().
            baseValues( new BaseValues() ).
            imageTemplate( "%s" ).
            info( info ).
            build().
            buildNewValues().
            get();
    }
}
