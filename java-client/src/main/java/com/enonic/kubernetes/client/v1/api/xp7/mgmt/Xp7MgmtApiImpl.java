package com.enonic.kubernetes.client.v1.api.xp7.mgmt;

import com.enonic.kubernetes.client.v1.api.xp7.TargetSelector;
import com.enonic.kubernetes.client.v1.api.xp7.Xp7ClientBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;

public class Xp7MgmtApiImpl implements Xp7MgmtApi {

    private final HttpClient client;
    private final Config config;

    public Xp7MgmtApiImpl(HttpClient client, Config config) {
        this.client = client;
        this.config = config;
    }

    public TargetSelector<Xp7MgmtApiSnapshots> snapshots() {
        return new SnapshotsBuilder(client, config);
    }

    public TargetSelector<Xp7MgmtApiRoutes> routes() {
        return new RoutesBuilder(client, config);
    }

    public TargetSelector<Xp7MgmtApiIdProviders> idProviders() {
        return new IdProvidersBuilder(client, config);
    }

    private static class SnapshotsBuilder extends Xp7ClientBuilder<Xp7MgmtApiSnapshots> {
        public SnapshotsBuilder(HttpClient client, Config config) {
            super(client, config);
        }

        @Override
        protected Xp7MgmtApiSnapshots build(HttpClient client, Config config, String namespace, String name, String nodeGroup) {
            return new Xp7MgmtApiSnapshotsImpl(client, config, namespace, name, nodeGroup);
        }
    }

    private static class RoutesBuilder extends Xp7ClientBuilder<Xp7MgmtApiRoutes> {
        public RoutesBuilder(HttpClient client, Config config) {
            super(client, config);
        }

        @Override
        protected Xp7MgmtApiRoutes build(HttpClient client, Config config, String namespace, String name, String nodeGroup) {
            return new Xp7MgmtApiRoutesImpl(client, config, namespace, name, nodeGroup);
        }
    }

    private static class IdProvidersBuilder extends Xp7ClientBuilder<Xp7MgmtApiIdProviders> {
        public IdProvidersBuilder(HttpClient client, Config config) {
            super(client, config);
        }

        @Override
        protected Xp7MgmtApiIdProviders build(HttpClient client, Config config, String namespace, String name, String nodeGroup) {
            return new Xp7MgmtApiIdProvidersImpl(client, config, namespace, name, nodeGroup);
        }
    }
}
