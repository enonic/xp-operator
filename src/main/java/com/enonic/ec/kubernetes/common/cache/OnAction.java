package com.enonic.ec.kubernetes.common.cache;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watcher;

@FunctionalInterface
public interface OnAction<T extends HasMetadata>
{
    void accept( Watcher.Action action, String id, Optional<T> oldResource, Optional<T> newResource );
}
