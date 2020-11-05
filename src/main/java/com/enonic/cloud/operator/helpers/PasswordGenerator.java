package com.enonic.cloud.operator.helpers;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PasswordGenerator
{
    private static final SecureRandom random = new SecureRandom();

    private static final List<Character> letters = new ArrayList<>();

    static
    {
        // Adding ASCII Decimal value from 33 to 126
        for (int i = 33; i < 127; i++) {
            letters.add((char) i);
        }
    }

    private static char getRandomChar()
    {
        return letters.get( random.nextInt( letters.size() ) );
    }

    public static String getPassword( int length )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < length; i++ )
        {
            sb.append( getRandomChar() );
        }
        return sb.toString();
    }

    public static void main( String[] args )
    {
        System.out.println( getPassword( 32 ) );
    }
}
