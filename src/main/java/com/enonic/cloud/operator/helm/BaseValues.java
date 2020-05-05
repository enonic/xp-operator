package com.enonic.cloud.operator.helm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;
import static com.enonic.cloud.operator.common.Configuration.globalConfig;

public class BaseValues
    extends HashMap<String, Object>
{
    private static final String keyPrefix = "operator.helm.charts.Values.";

    public BaseValues()
    {
        super();
        globalConfig().getPropertyNames().forEach( name -> {
            if ( name.startsWith( keyPrefix ) )
            {
                handleKey( this, name.replace( keyPrefix, "" ), cfgStr( name ) );
            }
        } );
    }

    @SuppressWarnings("unchecked")
    private static void handleKeySplit( Map<String, Object> values, final List<String> split, final String value )
    {
        Preconditions.checkState( split.size() > 0 );
        if ( split.size() == 1 )
        {
            String key = split.get( 0 );
            if ( value.equals( "false" ) )
            {
                values.put( key, false );
            }
            else if ( value.equals( "true" ) )
            {
                values.put( key, true );
            }
            else if ( value.contains( "," ) )
            {
                values.put( key, value.split( "," ) );
            }
            else
            {
                values.put( split.get( 0 ), value );
            }
        }
        else
        {
            Map<String, Object> tmp = (Map<String, Object>) values.getOrDefault( split.get( 0 ), new HashMap<>() );
            handleKeySplit( tmp, split.subList( 1, split.size() ), value );
            values.put( split.get( 0 ), tmp );
        }
    }

    private void handleKey( Map<String, Object> values, final String name, final String value )
    {
        handleKeySplit( values, createSplit( name ), value );
    }

    private List<String> createSplit( final String name )
    {
        List<Integer> quoteIndexes = new LinkedList<>();
        for ( int i = 0; i < name.length(); i++ )
        {
            if ( name.charAt( i ) == '"' )
            {
                quoteIndexes.add( i );
                if ( quoteIndexes.size() == 0 )
                {
                    break;
                }
            }
        }
        Preconditions.checkState( quoteIndexes.size() % 2 == 0, String.format( "Number of quotes in '%s' in not an event number", name ) );

        List<String> res = new LinkedList<>();

        if ( quoteIndexes.isEmpty() )
        {
            for ( String s : name.split( "\\." ) )
            {
                if ( !"".equals( s ) )
                {
                    res.add( s );
                }
            }
            return res;
        }

        res.addAll( createSplit( name.substring( 0, quoteIndexes.get( 0 ) ) ) );
        res.add( name.substring( quoteIndexes.get( 0 ) + 1, quoteIndexes.get( 1 ) ) );
        if ( quoteIndexes.get( 1 ) != name.length() - 1 )
        {
            res.addAll( createSplit( name.substring( quoteIndexes.get( 1 ) + 1 ) ) );
        }
        return res;
    }
}
