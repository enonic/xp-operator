package com.enonic.cloud.operator.helm.v1alpha2.xp7vhost;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.enonic.cloud.helm.values.BaseValues;
import com.enonic.cloud.helm.values.ValueBuilder;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.operator.helm.HelmTest;
import com.enonic.cloud.operator.v1alpha2xp7vhost.VHostHelmValueBuilderImpl;

@SuppressWarnings("WeakerAccess")
public class Xp7VHostTest
    extends HelmTest
{
    private final ValueBuilder<Xp7VHost> valueBuilder;

    public Xp7VHostTest()
    {
        super();
        valueBuilder = VHostHelmValueBuilderImpl.of( new BaseValues() );
    }

    @Override
    protected String chartToTest()
    {
        return "v1alpha2/xp7vhost";
    }

    @Override
    protected Object createValues( final ObjectMapper mapper, final File input )
        throws IOException
    {
        return valueBuilder.apply( mapper.readValue( input, Xp7VHost.class ) );
    }
}
