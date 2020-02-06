package com.enonic.ec.kubernetes.operator.operators.common.cache;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watcher;

@SuppressWarnings("WeakerAccess") // This has to be accessible from outside package
@FunctionalInterface
public interface OnAction<T extends HasMetadata>
{
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void accept( Watcher.Action action, String id, Optional<T> oldResource, Optional<T> newResource );
}
