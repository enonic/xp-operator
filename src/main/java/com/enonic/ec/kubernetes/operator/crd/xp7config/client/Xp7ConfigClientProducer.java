package com.enonic.ec.kubernetes.operator.crd.xp7config.client;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.crd.xp7config.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.crd.xp7config.Xp7ConfigResourceDoneable;
import com.enonic.ec.kubernetes.operator.crd.xp7config.Xp7ConfigResourceList;

import static com.enonic.ec.kubernetes.operator.crd.CrdClientsProducer.createCrdClient;

@Singleton
public class Xp7ConfigClientProducer
{
    private final Xp7ConfigClient xp7ConfigClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public Xp7ConfigClientProducer( DefaultClientProducer defaultClientProducer,
                                    @ConfigProperty(name = "operator.crd.xp.group") String group,
                                    @ConfigProperty(name = "operator.crd.xp.apiVersion") String apiVersion,
                                    @ConfigProperty(name = "operator.crd.xp.configs.kind") String kind,
                                    @ConfigProperty(name = "operator.crd.xp.configs.name") String name )
    {
        xp7ConfigClient = new Xp7ConfigClient(
            createCrdClient( defaultClientProducer.client(), group + "/" + apiVersion, kind, name, Xp7ConfigResource.class,
                             Xp7ConfigResourceList.class, Xp7ConfigResourceDoneable.class ) );
    }

    public Xp7ConfigClient produce()
    {
        return xp7ConfigClient;
    }
}
