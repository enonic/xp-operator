package com.enonic.cloud.kubernetes;

import java.util.Comparator;

import io.fabric8.kubernetes.api.model.HasMetadata;

public class Comparators
{
    public static Comparator<HasMetadata> namespace()
    {
        return Comparator.comparing( ( HasMetadata a ) -> a.getMetadata().getNamespace() );
    }

    public static Comparator<HasMetadata> name()
    {
        return Comparator.comparing( ( HasMetadata a ) -> a.getMetadata().getName() );
    }

    public static Comparator<HasMetadata> namespaceAndName()
    {
        return namespace().thenComparing( name() );
    }

    public static Comparator<HasMetadata> creationTime()
    {
        return Comparator.comparing( ( HasMetadata a ) -> a.getMetadata().getCreationTimestamp() );
    }
}
