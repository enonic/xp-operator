package com.enonic.kubernetes.client.apis;

public interface NodeGrouped<T> {
    T withNodeGroup(final String s);

    default T withAnyNode() {
        return withNodeGroup("all");
    }
}
