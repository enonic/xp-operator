package com.enonic.kubernetes.client.v1alpha1;

import com.enonic.kubernetes.client.Crd;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppSpec;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppStatus;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;


@Group("enonic.cloud")
@Version("v1alpha1")
@Kind("Xp7App")
public class Xp7App
        extends Crd<Xp7AppSpec, Xp7AppStatus>
        implements Namespaced {
    public static MixedOperation<Xp7App, Xp7AppList, Resource<Xp7App>> createCrdClient(final KubernetesClient client) {
        return client.customResources(Xp7App.class, Xp7AppList.class);
    }

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
