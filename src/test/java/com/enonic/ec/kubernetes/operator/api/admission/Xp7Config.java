package com.enonic.ec.kubernetes.operator.api.admission;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class Xp7Config
    extends TestAdmissionApi
{
    @TestFactory
    public Stream<DynamicTest> create()
    {
        return createStreamWithDeploymentCache( "xp7deploymentsCache.yaml", tests -> {
            tests.put( "xp7config/create/missingData.yaml", "Some fields in 'spec' are missing: [data]" );
            tests.put( "xp7config/create/missingFile.yaml", "Some fields in 'spec' are missing: [file]" );
            tests.put( "xp7config/create/missingNode.yaml", "Some fields in 'spec' are missing: [node]" );
            tests.put( "xp7config/create/missingSpec.yaml", "Old and new resource can not be empty, is 'spec' missing?" );
            tests.put( "xp7config/create/valid.yaml", null );
            tests.put( "xp7config/create/wrongNamespace.yaml", "XpDeployment 'wrongnamespace' not found" );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> update()
    {
        return createStreamWithDeploymentCache( "xp7deploymentsCache.yaml", tests -> {
            tests.put( "xp7config/update/valid.yaml", null );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> delete()
    {
        return createStreamWithDeploymentCache( "xp7deploymentsCache.yaml", tests -> {
            tests.put( "xp7config/delete/valid.yaml", null );
        } );
    }
}
