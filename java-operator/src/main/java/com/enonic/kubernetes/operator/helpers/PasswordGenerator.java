package com.enonic.kubernetes.operator.helpers;

import java.security.SecureRandom;

public class PasswordGenerator
{
    private static final SecureRandom random = new SecureRandom();

    // Set taken from LastPass password generation
    private static final String letters = "@!#$%&*^0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String getRandomScramble( String pool, int length )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < length; i++ )
        {
            sb.append( getRandomChar( pool ) );
        }
        return sb.toString();
    }

    private static char getRandomChar( String pool )
    {
        return pool.charAt( random.nextInt( pool.length() ) );
    }

    public static String getPassword( int length )
    {
        return getRandomScramble( letters, length );
    }

}
