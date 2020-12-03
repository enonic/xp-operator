package com.enonic.cloud.common;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;

public class Utils
{
    public static OwnerReference createOwnerReference( final HasMetadata hasMetadata )
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
