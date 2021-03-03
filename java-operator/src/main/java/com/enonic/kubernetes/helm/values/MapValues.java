package com.enonic.kubernetes.helm.values;

import java.util.HashMap;

public class MapValues
    extends HashMap<String, Object>
    implements Values
{
    public MapValues( final BaseValues baseValues )
    {
        super( baseValues );
    }
}
