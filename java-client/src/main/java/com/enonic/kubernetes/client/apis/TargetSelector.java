package com.enonic.kubernetes.client.apis;

public interface TargetSelector<T> extends Namespaced<Named<NodeGrouped<T>>> {
}
