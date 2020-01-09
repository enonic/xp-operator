package com.enonic.ec.kubernetes.operator.helm.v1alpha1.xp7deployment;

import java.io.File;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.enonic.ec.kubernetes.operator.helm.HelmTest;

public class Xp7DeploymentTest extends HelmTest
{
    @Override
    protected String chartToTest()
    {
        return "v1alpha2";
    }

    @Override
    protected Object createValues( final ObjectMapper mapper, final File input )
    {
        return Collections.emptyMap();
    }
}
