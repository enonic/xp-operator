package com.enonic.cloud.kubernetes.client.v1alpha2;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

import com.enonic.cloud.kubernetes.client.Crd;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentSpec;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentStatus;

@Group("enonic.cloud")
@Version("v1alpha2")
public class Xp7Deployment
    extends Crd<Xp7DeploymentSpec, Xp7DeploymentStatus>
    implements Namespaced
{
    public Xp7Deployment()
    {
        super();
        setApiVersion( "enonic.cloud/v1alpha2" );
        setKind( "Xp7Config" );
    }

    public static MixedOperation<Xp7Deployment, Xp7DeploymentList, Resource<Xp7Deployment>> createCrdClient( final KubernetesClient client )
    {
        return client.customResources( createCrdContext(), Xp7Deployment.class, Xp7DeploymentList.class );
    }

    public static CustomResourceDefinitionContext createCrdContext()
    {
        return new CustomResourceDefinitionContext.Builder().
            withGroup( "enonic.cloud" ).
            withVersion( "v1alpha2" ).
            withScope( "Namespaced" ).
            withName( "xp7deployments.enonic.cloud" ).
            withPlural( "xp7deployments" ).
            withKind( "Xp7Deployment" ).
            build();
    }

    public Xp7Deployment withSpec( final Xp7DeploymentSpec spec )
    {
        this.setSpec( spec );
        return this;
    }

    public Xp7Deployment withStatus( final Xp7DeploymentStatus status )
    {
        this.setStatus( status );
        return this;
    }

    public static class Xp7DeploymentList
        extends CustomResourceList<Xp7Deployment>
    {
    }
}
