package com.enonic.kubernetes.client.v1.domain;

import com.enonic.kubernetes.client.Crd;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("enonic.cloud")
@Version("v1")
@Kind("Domain")
public class Domain
    extends Crd<DomainSpec, DomainStatus>
{

    public Domain withSpec( final DomainSpec spec )
    {
        this.setSpec( spec );
        return this;
    }

    public Domain withStatus( final DomainStatus status )
    {
        this.setStatus( status );
        return this;
    }

    public static class DomainList
        extends DefaultKubernetesResourceList<Domain>
    {
    }
}
