package com.enonic.cloud.operator.functions;

import java.util.Comparator;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public class HasMetadataSorter
    implements Comparator<HasMetadata>
{
    @Override
    public int compare( final HasMetadata a, final HasMetadata b )
    {
        int res = a.getMetadata().getNamespace().compareTo( b.getMetadata().getNamespace() );
        if ( res == 0 )
        {
            res = a.getMetadata().getName().compareTo( b.getMetadata().getName() );
        }
        return res;
    }
}
