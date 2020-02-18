package com.enonic.cloud.operator.operators.common.cache;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watcher;

@SuppressWarnings("WeakerAccess") // This has to be accessible from outside package
@FunctionalInterface
public interface OnAction<T extends HasMetadata>
{
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void accept( final String actionId, final Watcher.Action action, final Optional<T> oldResource, final Optional<T> newResource );
}
