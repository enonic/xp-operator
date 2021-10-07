package com.enonic.kubernetes.client.apis.xp7.mgmt;

import com.enonic.kubernetes.client.apis.Named;
import com.enonic.kubernetes.client.apis.Namespaced;
import com.enonic.kubernetes.client.apis.NodeGrouped;
import com.enonic.kubernetes.client.apis.xp7.Xp7ClientBuilder;
import io.fabric8.kubernetes.client.Config;
import okhttp3.OkHttpClient;

public class Xp7MgmtApi {

    private final OkHttpClient client;
    private final Config config;

    public Xp7MgmtApi(OkHttpClient client, Config config) {
        this.client = client;
        this.config = config;
    }

    public Namespaced<Named<NodeGrouped<Xp7MgmtApiSnapshots>>> snapshots() {
        return new SnapshotsBuilder(client, config);
    }

    private static class SnapshotsBuilder extends Xp7ClientBuilder<Xp7MgmtApiSnapshots> {
        public SnapshotsBuilder(OkHttpClient client, Config config) {
            super(client, config);
        }

        @Override
        protected Xp7MgmtApiSnapshots build(OkHttpClient client, Config config, String namespace, String name, String nodeGroup) {
            return new Xp7MgmtApiSnapshots(client, config, namespace, name, nodeGroup);
        }
    }
}
