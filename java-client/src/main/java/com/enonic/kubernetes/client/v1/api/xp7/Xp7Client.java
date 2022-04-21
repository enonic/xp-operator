package com.enonic.kubernetes.client.v1.api.xp7;

import com.enonic.kubernetes.client.RawClient;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;

public abstract class Xp7Client extends RawClient {

    private final String namespace;
    private final String name;
    private final String nodeGroup;

    public Xp7Client(HttpClient client, Config config, final String namespace, final String name, final String nodeGroup) {
        super(client, config, "v1");
        this.namespace = namespace;
        this.name = name;
        this.nodeGroup = nodeGroup;
    }

    @Override
    protected String baseUrl() {
        return String.format("%s/%s/%s/%s/%s", super.baseUrl(), "xp7", namespace, name, nodeGroup);
    }
}
