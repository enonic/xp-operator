package com.enonic.cloud.operator.functions;

import java.util.function.Function;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;

public class CreateOwnerReference
    implements Function<HasMetadata, OwnerReference>
{
    @Override
    public OwnerReference apply( final HasMetadata hasMetadata )
    {
        return new OwnerReferenceBuilder().
            withApiVersion( hasMetadata.getApiVersion() ).
            withKind( hasMetadata.getKind() ).
            withUid( hasMetadata.getMetadata().getUid() ).
            withController( true ).
            withBlockOwnerDeletion( false ).
            withName( hasMetadata.getMetadata().getName() ).
            build();
    }
}
