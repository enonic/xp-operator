package com.enonic.ec.kubernetes.operator.crd.xp7vhost.client;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.ec.kubernetes.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResourceDoneable;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResourceList;

import static com.enonic.ec.kubernetes.operator.crd.CrdClientsProducer.createCrdClient;

@Singleton
public class Xp7VHostClientProducer
{
    private final Xp7VHostClient xp7VHostClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public Xp7VHostClientProducer( DefaultClientProducer defaultClientProducer, @ConfigProperty(name = "operator.crd.xp.group") String group,
                                   @ConfigProperty(name = "operator.crd.xp.apiVersion") String apiVersion,
                                   @ConfigProperty(name = "operator.crd.xp.vhosts.kind") String kind,
                                   @ConfigProperty(name = "operator.crd.xp.vhosts.name") String name )
    {
        xp7VHostClient = new Xp7VHostClient(
            createCrdClient( defaultClientProducer.client(), group + "/" + apiVersion, kind, name, Xp7VHostResource.class,
                             Xp7VHostResourceList.class, Xp7VHostResourceDoneable.class ) );
    }

    public Xp7VHostClient produce()
    {
        return xp7VHostClient;
    }
}
