package com.enonic.kubernetes.common;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class Preconditions
{
    private Preconditions()
    {
    }

    public static void checkState( final boolean expression )
    {
        if ( !expression )
        {
            throw new IllegalStateException();
        }
    }

    public static void checkState( final boolean expression, final String message, final Object... args )
    {
        if ( !expression )
        {
            throw new IllegalStateException( format( message, args ) );
        }
    }

    public static void checkArgument( final boolean expression, final String message, final Object... args )
    {
        if ( !expression )
        {
            throw new IllegalArgumentException( format( message, args ) );
        }
    }

    private static String format( final String message, final Object... args )
    {
        return args.length == 0 ? message : String.format( message, args );
    }
}
