package com.enonic.cloud.kubernetes.caches;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;

import static com.enonic.cloud.common.Configuration.cfgStr;

@Singleton
public class ConfigMapCache
    extends AbstractCache<ConfigMap, ConfigMapList>
{
    @Inject
    public ConfigMapCache( KubernetesClient client )
    {
        super( client.
            configMaps().
            inAnyNamespace().
            withLabel( cfgStr( "operator.helm.charts.Values.labelKeys.managed" ), "true" ) );
    }
}
