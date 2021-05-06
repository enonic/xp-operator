package com.enonic.kubernetes.client.v1alpha2;

import com.enonic.kubernetes.client.Crd;
import com.enonic.kubernetes.client.v1alpha2.xp7config.Xp7ConfigSpec;
import com.enonic.kubernetes.client.v1alpha2.xp7config.Xp7ConfigStatus;
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
@Kind("Xp7Config")
public class Xp7Config
    extends Crd<Xp7ConfigSpec, Xp7ConfigStatus>
    implements Namespaced
{
    public static MixedOperation<Xp7Config, Xp7ConfigList, Resource<Xp7Config>> createCrdClient( final KubernetesClient client )
    {
        return client.customResources( Xp7Config.class, Xp7ConfigList.class );
    }

    public Xp7Config withSpec( final Xp7ConfigSpec spec )
    {
        this.setSpec( spec );
        return this;
    }

    public Xp7Config withStatus( final Xp7ConfigStatus status )
    {
        this.setStatus( status );
        return this;
    }

    public static class Xp7ConfigList
        extends CustomResourceList<Xp7Config>
    {
    }
}
