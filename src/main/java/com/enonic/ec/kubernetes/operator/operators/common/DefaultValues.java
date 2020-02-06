package com.enonic.ec.kubernetes.operator.operators.common;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;

public abstract class DefaultValues
{
    protected Map<String, String> defaultLabels( HasMetadata resource )
    {
        Map<String, String> res = new HashMap<>( resource.getMetadata().getLabels() );
        res.put( "managedBy", "ec-operator" );
        return res;
    }
}
