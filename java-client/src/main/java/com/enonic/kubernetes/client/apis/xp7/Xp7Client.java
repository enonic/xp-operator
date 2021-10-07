package com.enonic.kubernetes.client.apis.xp7;

import com.enonic.kubernetes.client.apis.RawClient;
import io.fabric8.kubernetes.client.Config;
import okhttp3.OkHttpClient;

public abstract class Xp7Client extends RawClient {

    private final String namespace;
    private final String name;
    private final String nodeGroup;

    public Xp7Client(OkHttpClient client, Config config, final String namespace, final String name, final String nodeGroup) {
        super(client, config);
        this.namespace = namespace;
        this.name = name;
        this.nodeGroup = nodeGroup;
    }

    @Override
    protected String baseUrl() {
        return String.format("%s/%s/%s/%s/%s", super.baseUrl(), "xp7", namespace, name, nodeGroup);
    }
}
