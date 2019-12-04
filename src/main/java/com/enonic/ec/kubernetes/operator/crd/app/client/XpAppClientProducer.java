package com.enonic.ec.kubernetes.operator.crd.app.client;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResource;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResourceDoneable;
import com.enonic.ec.kubernetes.operator.crd.app.XpAppResourceList;

import static com.enonic.ec.kubernetes.operator.crd.CrdClientsProducer.createCrdClient;

@Singleton
public class XpAppClientProducer
{
    private final XpAppClient xpAppClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public XpAppClientProducer( DefaultClientProducer defaultClientProducer, @ConfigProperty(name = "operator.crd.xp.group") String group,
                                @ConfigProperty(name = "operator.crd.xp.apiVersion") String apiVersion,
                                @ConfigProperty(name = "operator.crd.xp.apps.kind") String kind,
                                @ConfigProperty(name = "operator.crd.xp.apps.name") String name )
    {
        xpAppClient = new XpAppClient(
            createCrdClient( defaultClientProducer.client(), group + "/" + apiVersion, kind, name, XpAppResource.class,
                             XpAppResourceList.class, XpAppResourceDoneable.class ) );
    }

    public XpAppClient produce()
    {
        return xpAppClient;
    }
}
