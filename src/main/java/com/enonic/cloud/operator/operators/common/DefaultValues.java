package com.enonic.cloud.operator.operators.common;

import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;

public abstract class DefaultValues
{
    protected Map<String, String> defaultLabels( HasMetadata resource )
    {
        return resource.getMetadata().getLabels();
    }
}
