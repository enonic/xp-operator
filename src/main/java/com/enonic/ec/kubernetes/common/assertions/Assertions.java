package com.enonic.ec.kubernetes.common.assertions;

public class Assertions
{
    public static <R> R assertNotNull( String name, R o )
        throws AssertionException
    {
        if ( o == null )
        {
            throw new AssertionException( "Field '" + name + "' cannot be null" );
        }
        return o;
    }

    public static <R> void assertEquals( String message, R a, R b )
    {
        if ( a == null && b == null )
        {
            return;
        }
        if ( a != null && a.equals( b ) )
        {
            return;
        }
        if ( b != null && b.equals( a ) )
        {
            return;
        }
        throw new AssertionException( message );
    }

    public static <R> R ifNullDefault( R o, R def )
    {
        if ( o == null )
        {
            return def;
        }
        return o;
    }
}
