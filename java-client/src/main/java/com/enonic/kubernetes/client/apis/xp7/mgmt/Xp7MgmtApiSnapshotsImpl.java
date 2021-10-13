package com.enonic.kubernetes.client.apis.xp7.mgmt;

import com.enonic.kubernetes.client.api.xp7.snapshots.Xp7MgmtSnapshotsList;
import io.fabric8.kubernetes.client.Config;
import okhttp3.OkHttpClient;

public class Xp7MgmtApiSnapshotsImpl extends Xp7MgmtClient implements Xp7MgmtApiSnapshots {
    public Xp7MgmtApiSnapshotsImpl(OkHttpClient client, Config config, String namespace, String name, String nodeGroup) {
        super(client, config, namespace, name, nodeGroup);
    }

    public Xp7MgmtSnapshotsList list() {
        return request(requestBuilder("repo/snapshot/list"), Xp7MgmtSnapshotsList.class);
    }
}
