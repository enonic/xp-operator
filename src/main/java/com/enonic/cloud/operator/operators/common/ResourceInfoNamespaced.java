package com.enonic.cloud.operator.operators.common;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.operator.common.info.Diff;
import com.enonic.cloud.operator.common.info.ResourceInfo;


public abstract class ResourceInfoNamespaced<T extends HasMetadata, D extends Diff<T>>
    extends ResourceInfo<T, D>
{
    @Value.Derived
    public String namespace()
    {
        return resource().getMetadata().getNamespace();
    }
}
