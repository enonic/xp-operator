package com.enonic.kubernetes.client.v1alpha2;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

import com.enonic.kubernetes.client.Crd;
import com.enonic.kubernetes.client.v1alpha2.domain.DomainSpec;
import com.enonic.kubernetes.client.v1alpha2.domain.DomainStatus;

@Group("enonic.cloud")
@Version("v1alpha2")
public class Domain
    extends Crd<DomainSpec, DomainStatus>
{
    public Domain()
    {
        super();
        setApiVersion( "enonic.cloud/v1alpha2" );
        setKind( "Domain" );
    }

    public static MixedOperation<Domain, DomainList, Resource<Domain>> createCrdClient( final KubernetesClient client )
    {
        return client.customResources( createCrdContext(), Domain.class, DomainList.class );
    }

    public static CustomResourceDefinitionContext createCrdContext()
    {
        return new CustomResourceDefinitionContext.Builder().
            withGroup( "enonic.cloud" ).
            withVersion( "v1alpha2" ).
            withScope( "Cluster" ).
            withName( "domains.enonic.cloud" ).
            withPlural( "domains" ).
            withKind( "Domain" ).
            build();
    }

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
        extends CustomResourceList<Domain>
    {
    }
}
