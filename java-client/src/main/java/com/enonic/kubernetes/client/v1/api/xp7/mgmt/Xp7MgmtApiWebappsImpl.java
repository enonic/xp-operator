package com.enonic.kubernetes.client.v1.api.xp7.mgmt;

import com.enonic.kubernetes.client.v1.api.xp7.webapps.Xp7MgmtWebapp;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;

import java.util.List;

public class Xp7MgmtApiWebappsImpl extends Xp7MgmtClient implements Xp7MgmtApiWebapps {
    public Xp7MgmtApiWebappsImpl(HttpClient client, Config config, String namespace, String name, String nodeGroup) {
        super(client, config, namespace, name, nodeGroup);
    }

    public List<Xp7MgmtWebapp> list() {
        return requestList( requestUrl( "webapps/list"), Xp7MgmtWebapp.class);
    }
}
