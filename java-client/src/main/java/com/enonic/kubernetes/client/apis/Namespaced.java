package com.enonic.kubernetes.client.apis;

public interface Namespaced<T> {
    T inNamespace(final String s);
}
