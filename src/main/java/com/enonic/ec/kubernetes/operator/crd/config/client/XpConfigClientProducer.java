package com.enonic.ec.kubernetes.operator.crd.config.client;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResource;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResourceDoneable;
import com.enonic.ec.kubernetes.operator.crd.config.XpConfigResourceList;

import static com.enonic.ec.kubernetes.operator.crd.CrdClientsProducer.createCrdClient;

@Singleton
public class XpConfigClientProducer
{
    private final XpConfigClient xpConfigClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public XpConfigClientProducer( DefaultClientProducer defaultClientProducer,
                                   @ConfigProperty(name = "operator.crd.xp.group") String group,
                                   @ConfigProperty(name = "operator.crd.xp.apiVersion") String apiVersion,
                                   @ConfigProperty(name = "operator.crd.xp.configs.kind") String kind,
                                   @ConfigProperty(name = "operator.crd.xp.configs.name") String name )
    {
        xpConfigClient = new XpConfigClient(
            createCrdClient( defaultClientProducer.client(), group + "/" + apiVersion, kind, name, XpConfigResource.class,
                             XpConfigResourceList.class, XpConfigResourceDoneable.class ) );
    }

    public XpConfigClient produce()
    {
        return xpConfigClient;
    }
}
