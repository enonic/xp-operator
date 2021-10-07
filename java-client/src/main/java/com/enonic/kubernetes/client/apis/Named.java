package com.enonic.kubernetes.client.apis;

public interface Named<T> {
    T withName(final String s);
}
