package com.enonic.kubernetes.operator.helm.v1.xp7deployment;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceAccount;

import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;
import com.enonic.kubernetes.helm.values.BaseValues;
import com.enonic.kubernetes.helm.values.ValueBuilder;
import com.enonic.kubernetes.operator.helm.HelmTest;
import com.enonic.kubernetes.operator.xp7deployment.OperatorXp7DeploymentHelm;

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
        return "v1/xp7deployment";
    }

    @Override
    protected Object createValues( final ObjectMapper mapper, final File input )
        throws IOException
    {
        return valueBuilder.apply( mapper.readValue( input, Xp7Deployment.class ) );
    }
}
