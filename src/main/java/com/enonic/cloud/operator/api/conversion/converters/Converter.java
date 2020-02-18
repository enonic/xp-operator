package com.enonic.cloud.operator.api.conversion.converters;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface Converter<S extends HasMetadata, R extends HasMetadata>
{
    Class<S> consumes();

    String produces();

    R convert( S source );
}
