package com.enonic.kubernetes.client.v1.xp7config;

import com.enonic.kubernetes.client.Crd;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("enonic.cloud")
@Version("v1")
@Kind("Xp7Config")
public class Xp7Config
        extends Crd<Xp7ConfigSpec, Xp7ConfigStatus>
        implements Namespaced {

    public Xp7Config withSpec(final Xp7ConfigSpec spec) {
        this.setSpec(spec);
        return this;
    }

    public Xp7Config withStatus(final Xp7ConfigStatus status) {
        this.setStatus(status);
        return this;
    }

    public static class Xp7ConfigList
            extends CustomResourceList<Xp7Config> {
    }
}
