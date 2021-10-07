package com.enonic.kubernetes.client.apis.xp7.mgmt;

import com.enonic.kubernetes.client.api.xp7.snapshots.Xp7MgmtSnapshotsList;
import io.fabric8.kubernetes.client.Config;
import okhttp3.OkHttpClient;

public class Xp7MgmtApiSnapshots extends Xp7MgmtClient {

    public Xp7MgmtApiSnapshots(OkHttpClient client, Config config, String namespace, String name, String nodeGroup) {
        super(client, config, namespace, name, nodeGroup);
    }

    public Xp7MgmtSnapshotsList list() {
        return request(requestBuilder("repo/snapshot/list"), Xp7MgmtSnapshotsList.class);
    }
}
