package com.enonic.kubernetes.client.v1.api.xp7;

public interface NodeGrouped<T> {
    T withNodeGroup(final String s);

    default T withAnyNode() {
        return withNodeGroup("all");
    }
}
