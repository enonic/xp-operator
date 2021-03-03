package com.enonic.kubernetes.operator.helpers;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;

import static java.util.stream.Collectors.groupingBy;

public abstract class HandlerConfig<R extends HasMetadata>
    implements Runnable
{
    @Override
    public void run()
    {
        // Collect affected namespaces and run update on each
        this.resourceStream().collect( groupingBy( r -> r.getMetadata().getNamespace() ) ).
            keySet().
            forEach( this::handle );
    }

    protected abstract Stream<R> resourceStream();

    protected abstract void handle( String namespace );
}
