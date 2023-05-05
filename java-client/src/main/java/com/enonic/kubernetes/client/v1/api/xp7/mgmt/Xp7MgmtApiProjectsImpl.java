package com.enonic.kubernetes.client.v1.api.xp7.mgmt;

import com.enonic.kubernetes.client.v1.api.xp7.projects.Xp7MgmtProject;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;

import java.util.List;

public class Xp7MgmtApiProjectsImpl extends Xp7MgmtClient implements Xp7MgmtApiProjects {
    public Xp7MgmtApiProjectsImpl(HttpClient client, Config config, String namespace, String name, String nodeGroup) {
        super(client, config, namespace, name, nodeGroup);
    }

    public List<Xp7MgmtProject> list() {
        return requestList( requestUrl( "content/projects/list"), Xp7MgmtProject.class);
    }
}
