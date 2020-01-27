package com.enonic.ec.kubernetes.operator.common.info;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import static com.enonic.ec.kubernetes.operator.common.Configuration.cfgStr;
import static com.enonic.ec.kubernetes.operator.common.Configuration.globalConfig;

public abstract class ValueBuilder<R extends ResourceInfo>
{
    private static String keyPrefix = "operator.helm.charts.Values.";

    public abstract R info();

    @Value.Derived
    public Object values()
    {
        Map<String, Object> values = new HashMap<>();
        globalConfig().getPropertyNames().forEach( name -> {
            if ( name.startsWith( keyPrefix ) )
            {
                handleKey( values, name.replace( keyPrefix, "" ), cfgStr( name ) );
            }
        } );
        createValues( values );
        return values;
    }

    private void handleKey( Map<String, Object> values, final String name, final String value )
    {
        handleKeySplit( values, Arrays.asList( name.split( "\\." ) ), value );
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
                values.put( key, false );
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

    protected abstract void createValues( final Map<String, Object> values );
}
