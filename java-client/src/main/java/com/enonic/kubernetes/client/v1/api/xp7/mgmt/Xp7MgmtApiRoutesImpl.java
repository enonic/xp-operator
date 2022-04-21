package com.enonic.kubernetes.client.v1.api.xp7.mgmt;

import com.enonic.kubernetes.client.v1.api.xp7.routes.Xp7MgmtRoutesList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;

public class Xp7MgmtApiRoutesImpl extends Xp7MgmtClient implements Xp7MgmtApiRoutes {
    public Xp7MgmtApiRoutesImpl(HttpClient client, Config config, String namespace, String name, String nodeGroup) {
        super(client, config, namespace, name, nodeGroup);
    }

    public Xp7MgmtRoutesList list() {
        return request(requestUrlBuilder("cloud-utils/routes"), Xp7MgmtRoutesList.class);
    }
}
