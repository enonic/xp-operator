package com.enonic.kubernetes.client.v1.api.xp7;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;

public abstract class Xp7ClientBuilder<T> implements TargetSelector<T>, Named<NodeGrouped<T>>, NodeGrouped<T> {

    private final HttpClient client;
    private final Config config;
    private String namespace;
    private String name;

    public Xp7ClientBuilder(HttpClient client, Config config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public Named<NodeGrouped<T>> inNamespace(String s) {
        namespace = s;
        return this;
    }

    @Override
    public NodeGrouped<T> withName(String s) {
        name = s;
        return this;
    }

    @Override
    public T withNodeGroup(String s) {
        return build(client, config, namespace, name, s);
    }

    protected abstract T build(HttpClient client, Config config, String namespace, String name, String nodeGroup);
}
