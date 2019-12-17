package com.enonic.ec.kubernetes.operator.crd.xp7app.client;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResource;
import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResourceDoneable;
import com.enonic.ec.kubernetes.operator.crd.xp7app.Xp7AppResourceList;

import static com.enonic.ec.kubernetes.operator.crd.CrdClientsProducer.createCrdClient;

@Singleton
public class Xp7AppClientProducer
{
    private final Xp7AppClient xp7AppClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public Xp7AppClientProducer( DefaultClientProducer defaultClientProducer, @ConfigProperty(name = "operator.crd.xp.group") String group,
                                 @ConfigProperty(name = "operator.crd.xp.apiVersion") String apiVersion,
                                 @ConfigProperty(name = "operator.crd.xp.apps.kind") String kind,
                                 @ConfigProperty(name = "operator.crd.xp.apps.name") String name )
    {
        xp7AppClient = new Xp7AppClient(
            createCrdClient( defaultClientProducer.client(), group + "/" + apiVersion, kind, name, Xp7AppResource.class,
                             Xp7AppResourceList.class, Xp7AppResourceDoneable.class ) );
    }

    public Xp7AppClient produce()
    {
        return xp7AppClient;
    }
}
