package com.enonic.ec.kubernetes.operator.helm;

import java.util.HashMap;
import java.util.Map;

import com.enonic.ec.kubernetes.operator.common.Configuration;

public class ValuesBuilder
    extends Configuration
{
    private static String keyPrefix = "operator.helm.charts.values.";

    private Map<String, Object> map;

    private ValuesBuilder()
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

    public ValuesBuilder add( String key, Object value )
    {
        map.put( key, value );
        return this;
    }

    public Object build()
    {
        return map;
    }

    public static ValuesBuilder builder()
    {
        return new ValuesBuilder();
    }

}
