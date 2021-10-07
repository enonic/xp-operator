package com.enonic.kubernetes.client.apis.xp7.mgmt;

import com.enonic.kubernetes.client.api.xp7.routes.Xp7MgmtRoutesList;
import io.fabric8.kubernetes.client.Config;
import okhttp3.OkHttpClient;

public class Xp7MgmtApiRoutes extends Xp7MgmtClient {

    public Xp7MgmtApiRoutes(OkHttpClient client, Config config, String namespace, String name, String nodeGroup) {
        super(client, config, namespace, name, nodeGroup);
    }

    public Xp7MgmtRoutesList list() {
        return request(requestBuilder("cloud-utils/routes"), Xp7MgmtRoutesList.class);
    }
}
