package com.enonic.cloud.kubernetes.caches;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;

import static com.enonic.cloud.common.Configuration.cfgStr;

@Singleton
public class PodCache
    extends AbstractCache<Pod, PodList>
{
    @Inject
    public PodCache( KubernetesClient client )
    {
        super( client.
            pods().
            inAnyNamespace().
            withLabel( cfgStr( "operator.helm.charts.Values.labelKeys.managed" ), "true" ) );
    }
}
