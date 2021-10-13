package com.enonic.kubernetes.client.v1alpha2;

import com.enonic.kubernetes.client.Crd;
import com.enonic.kubernetes.client.v1alpha2.domain.DomainSpec;
import com.enonic.kubernetes.client.v1alpha2.domain.DomainStatus;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("enonic.cloud")
@Version("v1alpha2")
@Kind("Domain")
public class Domain
        extends Crd<DomainSpec, DomainStatus> {
    public static MixedOperation<Domain, DomainList, Resource<Domain>> createCrdClient(final KubernetesClient client) {
        return client.customResources(Domain.class, DomainList.class);
    }

    public Domain withSpec(final DomainSpec spec) {
        this.setSpec(spec);
        return this;
    }

    public Domain withStatus(final DomainStatus status) {
        this.setStatus(status);
        return this;
    }

    public static class DomainList
            extends CustomResourceList<Domain> {
    }
}
