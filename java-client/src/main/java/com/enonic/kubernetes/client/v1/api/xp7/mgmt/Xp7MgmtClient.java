package com.enonic.kubernetes.client.v1.api.xp7.mgmt;

import com.enonic.kubernetes.client.v1.api.xp7.Xp7Client;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;

public abstract class Xp7MgmtClient extends Xp7Client {

    public Xp7MgmtClient(HttpClient client, Config config, final String namespace, final String name, final String nodeGroup) {
        super(client, config, namespace, name, nodeGroup);
    }

    @Override
    protected String baseUrl() {
        return String.format("%s/%s", super.baseUrl(), "mgmt");
    }
}
