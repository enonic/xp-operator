package com.enonic.ec.kubernetes.operator.helm;

import java.util.HashMap;
import java.util.Map;

import com.enonic.ec.kubernetes.operator.common.Configuration;

public abstract class ValuesBuilder
    extends Configuration
{
    private static String keyPrefix = "operator.helm.charts.values.";

    protected Map<String, Object> map;

    protected ValuesBuilder()
    {
        this.map = new HashMap<>();
        addProperties();
    }

    private void addProperties()
    {
        globalConfig().getPropertyNames().forEach( name -> {
            if ( name.startsWith( keyPrefix ) )
            {
                map.put( name.replace( keyPrefix, "" ), cfgStr( name ) );
            }
        } );
    }

    protected ValuesBuilder add( String key, Object value )
    {
        map.put( key, value );
        return this;
    }

    public abstract Object build();
}
