package com.enonic.kubernetes.client.v1alpha2;

import com.enonic.kubernetes.client.Crd;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpec;
import com.enonic.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentStatus;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("enonic.cloud")
@Version("v1alpha2")
@Kind("Xp7Deployment")
public class Xp7Deployment
        extends Crd<Xp7DeploymentSpec, Xp7DeploymentStatus>
        implements Namespaced {

    public static MixedOperation<Xp7Deployment, Xp7DeploymentList, Resource<Xp7Deployment>> createCrdClient(final KubernetesClient client) {
        return client.customResources(Xp7Deployment.class, Xp7DeploymentList.class);
    }

    public Xp7Deployment withSpec(final Xp7DeploymentSpec spec) {
        this.setSpec(spec);
        return this;
    }

    public Xp7Deployment withStatus(final Xp7DeploymentStatus status) {
        this.setStatus(status);
        return this;
    }

    public static class Xp7DeploymentList
            extends CustomResourceList<Xp7Deployment> {
    }
}
