package com.enonic.kubernetes.client.v1.xp7app;

import com.enonic.kubernetes.client.Crd;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;


@Group("enonic.cloud")
@Version("v1")
@Kind("Xp7App")
public class Xp7App
        extends Crd<Xp7AppSpec, Xp7AppStatus>
        implements Namespaced {

    public Xp7App withSpec(final Xp7AppSpec spec) {
        this.setSpec(spec);
        return this;
    }

    public Xp7App withStatus(final Xp7AppStatus status) {
        this.setStatus(status);
        return this;
    }

    public static class Xp7AppList
            extends CustomResourceList<Xp7App> {
    }
}
