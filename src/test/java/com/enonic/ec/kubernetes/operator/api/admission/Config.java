package com.enonic.ec.kubernetes.operator.api.admission;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class Config
    extends AdmissionApiTest
{
    @TestFactory
    public Stream<DynamicTest> create()
    {
        return createStreamWithDeploymentCache( "deploymentsCache.yaml", tests -> {
            tests.put( "config/create/missingData.yaml", "Some fields in 'spec' are missing: [data]" );
            tests.put( "config/create/missingFile.yaml", "Some fields in 'spec' are missing: [file]" );
            tests.put( "config/create/missingNode.yaml", "Some fields in 'spec' are missing: [node]" );
            tests.put( "config/create/missingSpec.yaml", "Old and new resource can not be empty, is 'spec' missing?" );
            tests.put( "config/create/valid.yaml", null );
            tests.put( "config/create/wrongNamespace.yaml", "XpDeployment 'wrongnamespace' not found" );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> update()
    {
        return createStreamWithDeploymentCache( "deploymentsCache.yaml", tests -> {
            tests.put( "config/update/valid.yaml", null );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> delete()
    {
        return createStreamWithDeploymentCache( "deploymentsCache.yaml", tests -> {
            tests.put( "config/delete/valid.yaml", null );
        } );
    }
}
