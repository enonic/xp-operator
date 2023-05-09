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

    public TargetSelector<Xp7MgmtApiIdProviders> idProviders() {
        return new IdProvidersBuilder(client, config);
    }

    public TargetSelector<Xp7MgmtApiProjects> projects() {
        return new ProjectsBuilder(client, config);
    }

    public TargetSelector<Xp7MgmtApiWebapps> webapps() {
        return new WebappsBuilder(client, config);
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

    private static class IdProvidersBuilder extends Xp7ClientBuilder<Xp7MgmtApiIdProviders> {
        public IdProvidersBuilder(HttpClient client, Config config) {
            super(client, config);
        }

        @Override
        protected Xp7MgmtApiIdProviders build(HttpClient client, Config config, String namespace, String name, String nodeGroup) {
            return new Xp7MgmtApiIdProvidersImpl(client, config, namespace, name, nodeGroup);
        }
    }

    private static class ProjectsBuilder extends Xp7ClientBuilder<Xp7MgmtApiProjects> {
        public ProjectsBuilder(HttpClient client, Config config) {
            super(client, config);
        }

        @Override
        protected Xp7MgmtApiProjects build(HttpClient client, Config config, String namespace, String name, String nodeGroup) {
            return new Xp7MgmtApiProjectsImpl( client, config, namespace, name, nodeGroup);
        }
    }

    private static class WebappsBuilder extends Xp7ClientBuilder<Xp7MgmtApiWebapps> {
        public WebappsBuilder(HttpClient client, Config config) {
            super(client, config);
        }

        @Override
        protected Xp7MgmtApiWebapps build(HttpClient client, Config config, String namespace, String name, String nodeGroup) {
            return new Xp7MgmtApiWebappsImpl( client, config, namespace, name, nodeGroup);
        }
    }
}
