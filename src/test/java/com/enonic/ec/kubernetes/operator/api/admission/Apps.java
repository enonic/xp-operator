package com.enonic.ec.kubernetes.operator.api.admission;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class Apps
    extends AdmissionApiTest
{
    @TestFactory
    public Stream<DynamicTest> create()
    {
        return createStreamWithDeploymentCache( "deploymentsCache.yaml", tests -> {
            tests.put( "app/create/missingSpec.yaml", "Old and new resource can not be empty, is 'spec' missing?" );
            tests.put( "app/create/missingUrl.yaml", "Old and new resource can not be empty, is 'spec' missing?" );
            tests.put( "app/create/valid.yaml", null );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> update()
    {
        return createStreamWithDeploymentCache( "deploymentsCache.yaml", tests -> {
            tests.put( "app/update/valid.yaml", null );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> delete()
    {
        return createStreamWithDeploymentCache( "deploymentsCache.yaml", tests -> {
            tests.put( "app/delete/valid.yaml", null );
        } );
    }
}
