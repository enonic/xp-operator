package com.enonic.kubernetes.client.v1.api.xp7.mgmt;

import com.enonic.kubernetes.client.v1.api.xp7.snapshots.Xp7MgmtSnapshotsList;
import com.enonic.kubernetes.client.v1.api.xp7.snapshots.Xp7MgmtSnapshotsListSnapshot;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class Xp7MgmtApiSnapshotsImpl extends Xp7MgmtClient implements Xp7MgmtApiSnapshots {

    private static final Logger LOG = LoggerFactory.getLogger( Xp7MgmtApiSnapshotsImpl.class );

    public Xp7MgmtApiSnapshotsImpl(HttpClient client, Config config, String namespace, String name, String nodeGroup) {
        super(client, config, namespace, name, nodeGroup);
    }

    public Xp7MgmtSnapshotsList list() {
        return request( requestUrl( "repo/snapshots/list"), Xp7MgmtSnapshotsList.class);
    }
}
