package com.enonic.kubernetes.client.v1alpha2;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

import com.enonic.kubernetes.client.Crd;
import com.enonic.kubernetes.client.v1alpha2.xp7config.Xp7ConfigSpec;
import com.enonic.kubernetes.client.v1alpha2.xp7config.Xp7ConfigStatus;

@Group("enonic.cloud")
@Version("v1alpha2")
public class Xp7Config
    extends Crd<Xp7ConfigSpec, Xp7ConfigStatus>
    implements Namespaced
{
    public Xp7Config()
    {
        super();
        setApiVersion( "enonic.cloud/v1alpha2" );
        setKind( "Xp7Config" );
    }

    public static MixedOperation<Xp7Config, Xp7ConfigList, Resource<Xp7Config>> createCrdClient( final KubernetesClient client )
    {
        return client.customResources( createCrdContext(), Xp7Config.class, Xp7ConfigList.class );
    }

    public static CustomResourceDefinitionContext createCrdContext()
    {
        return new CustomResourceDefinitionContext.Builder().
            withGroup( "enonic.cloud" ).
            withVersion( "v1alpha2" ).
            withScope( "Namespaced" ).
            withName( "xp7configs.enonic.cloud" ).
            withPlural( "xp7configs" ).
            withKind( "Xp7Config" ).
            build();
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
