package com.enonic.kubernetes.client.v1.api.xp7;

public interface Namespaced<T> {
    T inNamespace(final String s);
}
