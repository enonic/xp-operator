package com.enonic.cloud.operator.helm.v1alpha2.xp7deployment;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceAccount;

import com.enonic.cloud.helm.values.BaseValues;
import com.enonic.cloud.helm.values.ValueBuilder;
import com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.cloud.operator.helm.HelmTest;
import com.enonic.cloud.operator.v1alpha2xp7deployment.OperatorXp7DeploymentHelm;

@SuppressWarnings("WeakerAccess")
public class Xp7DeploymentTest
    extends HelmTest
{
    final ValueBuilder<Xp7Deployment> valueBuilder;

    public Xp7DeploymentTest()
    {
        super();
        valueBuilder = new OperatorXp7DeploymentHelm.Xp7DeploymentValueBuilder( new BaseValues( "44ddc40b-266c-4c99-b094-e758328fc6ba" ),
                                                                                () -> "password", this::createTestSa );
    }

    private ServiceAccount createTestSa()
    {
        ServiceAccount sa = new ServiceAccount();
        ObjectMeta meta = new ObjectMeta();
        meta.setName( "cloudApi" );
        meta.setNamespace( "ec-system" );
        sa.setMetadata( meta );
        return sa;
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
        return valueBuilder.apply( mapper.readValue( input, Xp7Deployment.class ) );
    }
}
