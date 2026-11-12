package com.enonic.kubernetes.client.v1.xp7deployment;

import com.enonic.kubernetes.client.Crd;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("enonic.cloud")
@Version("v1")
@Kind("Xp7Deployment")
public class Xp7Deployment
    extends Crd<Xp7DeploymentSpec, Xp7DeploymentStatus>
    implements Namespaced
{

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
        extends DefaultKubernetesResourceList<Xp7Deployment>
    {
    }
}
