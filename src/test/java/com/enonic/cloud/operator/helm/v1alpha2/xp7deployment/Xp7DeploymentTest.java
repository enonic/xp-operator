package com.enonic.cloud.operator.helm.v1alpha2.xp7deployment;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.enonic.cloud.helm.values.BaseValues;
import com.enonic.cloud.helm.values.ValueBuilder;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.helm.HelmTest;
import com.enonic.cloud.operator.v1alpha2xp7deployment.DeploymentHelmValueBuilderImpl;

@SuppressWarnings("WeakerAccess")
public class Xp7DeploymentTest
    extends HelmTest
{
    final ValueBuilder<V1alpha2Xp7Deployment> valueBuilder;

    public Xp7DeploymentTest()
    {
        super();
        valueBuilder = DeploymentHelmValueBuilderImpl.of( new BaseValues(), () -> "password", () -> null );
    }

    @Override
    protected String chartToTest()
    {
        return "v1alpha2/xp7deployment";
    }

    @Override
    protected Object createValues( final ObjectMapper mapper, final File input )
        throws IOException
    {
        return valueBuilder.apply( mapper.readValue( input, V1alpha2Xp7Deployment.class ) );
    }
}
