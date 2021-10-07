package com.enonic.kubernetes.client.apis.xp7.mgmt;

import com.enonic.kubernetes.client.apis.xp7.Xp7Client;
import io.fabric8.kubernetes.client.Config;
import okhttp3.OkHttpClient;

public abstract class Xp7MgmtClient extends Xp7Client {

    public Xp7MgmtClient(OkHttpClient client, Config config, final String namespace, final String name, final String nodeGroup) {
        super(client, config, namespace, name, nodeGroup);
    }

    @Override
    protected String baseUrl() {
        return String.format("%s/%s", super.baseUrl(), "mgmt");
    }
}
