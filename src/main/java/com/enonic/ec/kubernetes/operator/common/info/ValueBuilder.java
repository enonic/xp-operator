package com.enonic.ec.kubernetes.operator.common.info;

import java.util.HashMap;
import java.util.Map;

import org.immutables.value.Value;

import static com.enonic.ec.kubernetes.operator.common.Configuration.cfgStr;
import static com.enonic.ec.kubernetes.operator.common.Configuration.globalConfig;

public abstract class ValueBuilder<R extends ResourceInfo>
{
    private static String keyPrefix = "operator.helm.charts.values.";

    public abstract R info();

    @Value.Derived
    public Object values()
    {
        Map<String, Object> values = new HashMap<>();
        globalConfig().getPropertyNames().forEach( name -> {
            if ( name.startsWith( keyPrefix ) )
            {
                values.put( name.replace( keyPrefix, "" ), cfgStr( name ) );
            }
        } );
        createValues( values );
        return values;
    }

    protected abstract void createValues( final Map<String, Object> values );
}
