package com.enonic.cloud.operator.helm.v1alpha2.xp7vhost;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.operator.helm.BaseValues;
import com.enonic.cloud.operator.helm.HelmTest;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.ImmutableXp7VHostValues;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.ImmutableInfoXp7VHost;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.InfoXp7VHost;

public class Xp7VHostTest
    extends HelmTest
{
    @Override
    protected String chartToTest()
    {
        return "v1alpha2/xp7vhost";
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    protected Object createValues( final ObjectMapper mapper, final File input )
        throws IOException
    {
        V1alpha2Xp7Deployment deployment =
            mapper.readValue( getFile( HelmTest.class, "v1alpha2/test_deployment.yaml" ), V1alpha2Xp7Deployment.class );

        V1alpha2Xp7VHost resource = mapper.readValue( input, V1alpha2Xp7VHost.class );
        InfoXp7VHost info = ImmutableInfoXp7VHost.builder().
            newResource( resource ).
            overrideDeployment( deployment ).
            build();
        return ImmutableXp7VHostValues.builder().
            baseValues( new BaseValues() ).
            info( info ).
            build().
            buildNewValues().
            get();
    }
}
