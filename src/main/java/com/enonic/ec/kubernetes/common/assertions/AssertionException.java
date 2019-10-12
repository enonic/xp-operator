package com.enonic.ec.kubernetes.common.assertions;

class AssertionException
    extends RuntimeException
{
    public AssertionException( final String message )
    {
        super( message );
    }
}
