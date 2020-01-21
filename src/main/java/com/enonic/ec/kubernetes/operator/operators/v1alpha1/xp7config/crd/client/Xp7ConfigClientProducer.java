package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.client;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.enonic.ec.kubernetes.operator.common.client.DefaultClientProducer;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResourceDoneable;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7config.crd.Xp7ConfigResourceList;

import static com.enonic.ec.kubernetes.operator.crd.CrdClientsProducer.createCrdClient;

@Singleton
public class Xp7ConfigClientProducer
{
    private final Xp7ConfigClient xp7ConfigClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public Xp7ConfigClientProducer( DefaultClientProducer defaultClientProducer,
                                    @ConfigProperty(name = "operator.crd.v1alpha1.group") String group,
                                    @ConfigProperty(name = "operator.crd.v1alpha1.apiVersion") String apiVersion,
                                    @ConfigProperty(name = "operator.crd.v1alpha1.configs.kind") String kind,
                                    @ConfigProperty(name = "operator.crd.v1alpha1.configs.name") String name )
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
