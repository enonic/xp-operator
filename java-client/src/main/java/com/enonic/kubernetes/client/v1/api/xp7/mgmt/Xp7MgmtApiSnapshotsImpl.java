package com.enonic.kubernetes.client.v1.api.xp7.mgmt;

import com.enonic.kubernetes.client.v1.api.xp7.snapshots.Xp7MgmtSnapshotsList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;

public class Xp7MgmtApiSnapshotsImpl extends Xp7MgmtClient implements Xp7MgmtApiSnapshots {
    public Xp7MgmtApiSnapshotsImpl(HttpClient client, Config config, String namespace, String name, String nodeGroup) {
        super(client, config, namespace, name, nodeGroup);
    }

    public Xp7MgmtSnapshotsList list() {
        return request( requestUrl( "repo/snapshot/list"), Xp7MgmtSnapshotsList.class);
    }
}
