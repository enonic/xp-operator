package com.enonic.ec.kubernetes.crd.vhost.client;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResource;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResourceDoneable;
import com.enonic.ec.kubernetes.crd.vhost.XpVHostResourceList;

import static com.enonic.ec.kubernetes.crd.CrdClientsProducer.createCrdClient;

@Singleton
public class XpVHostClientProducer
{
    private final XpVHostClient xpVHostClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public XpVHostClientProducer( DefaultClientProducer defaultClientProducer,
                                  @ConfigProperty(name = "operator.crd.xp.apiVersion") String apiVersion,
                                  @ConfigProperty(name = "operator.crd.xp.vhosts.kind") String kind,
                                  @ConfigProperty(name = "operator.crd.xp.vhosts.name") String name )
    {
        xpVHostClient = new XpVHostClient(
            createCrdClient( defaultClientProducer.client(), apiVersion, kind, name, XpVHostResource.class, XpVHostResourceList.class,
                             XpVHostResourceDoneable.class ) );
    }

    public XpVHostClient produce()
    {
        return xpVHostClient;
    }
}
