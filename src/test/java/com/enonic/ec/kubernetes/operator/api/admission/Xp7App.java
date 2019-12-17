package com.enonic.ec.kubernetes.operator.api.admission;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class Xp7App
    extends TestAdmissionApi
{
    @TestFactory
    public Stream<DynamicTest> create()
    {
        return createStreamWithDeploymentCache( "xp7deploymentsCache.yaml", tests -> {
            tests.put( "xp7app/create/missingSpec.yaml", "Old and new resource can not be empty, is 'spec' missing?" );
            tests.put( "xp7app/create/missingUrl.yaml", "Old and new resource can not be empty, is 'spec' missing?" );
            tests.put( "xp7app/create/valid.yaml", null );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> update()
    {
        return createStreamWithDeploymentCache( "xp7deploymentsCache.yaml", tests -> {
            tests.put( "xp7app/update/valid.yaml", null );
        } );
    }

    @TestFactory
    public Stream<DynamicTest> delete()
    {
        return createStreamWithDeploymentCache( "xp7deploymentsCache.yaml", tests -> {
            tests.put( "xp7app/delete/valid.yaml", null );
        } );
    }
}
