package com.enonic.kubernetes.client.v1alpha1;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

import com.enonic.kubernetes.client.Crd;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppSpec;
import com.enonic.kubernetes.client.v1alpha1.xp7app.Xp7AppStatus;


@Group("enonic.cloud")
@Version("v1alpha1")
public class Xp7App
    extends Crd<Xp7AppSpec, Xp7AppStatus>
    implements Namespaced
{
    public Xp7App()
    {
        super();
        setApiVersion( "enonic.cloud/v1alpha1" );
        setKind( "Xp7App" );
    }

    public static MixedOperation<Xp7App, Xp7AppList, Resource<Xp7App>> createCrdClient( final KubernetesClient client )
    {
        return client.customResources( createCrdContext(), Xp7App.class, Xp7AppList.class );
    }

    public static CustomResourceDefinitionContext createCrdContext()
    {
        return new CustomResourceDefinitionContext.Builder().
            withGroup( "enonic.cloud" ).
            withVersion( "v1alpha1" ).
            withScope( "Namespaced" ).
            withName( "xp7apps.enonic.cloud" ).
            withPlural( "xp7apps" ).
            withKind( "Xp7App" ).
            build();
    }

    public Xp7App withSpec( final Xp7AppSpec spec )
    {
        this.setSpec( spec );
        return this;
    }

    public Xp7App withStatus( final Xp7AppStatus status )
    {
        this.setStatus( status );
        return this;
    }

    public static class Xp7AppList
        extends CustomResourceList<Xp7App>
    {
    }
}
