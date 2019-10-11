package com.enonic.ec.kubernetes.common.assertions;

public class AssertionException
    extends RuntimeException
{
    public AssertionException( final String message )
    {
        super( message );
    }
}
