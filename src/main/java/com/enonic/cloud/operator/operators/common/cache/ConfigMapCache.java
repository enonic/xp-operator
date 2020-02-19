package com.enonic.cloud.operator.operators.common.cache;

import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;

import com.enonic.cloud.operator.operators.common.clients.Clients;

@Singleton
public class ConfigMapCache
    extends Cache<ConfigMap, ConfigMapList>
{
    @Inject
    public ConfigMapCache( Clients clients )
    {
        super( Executors.newSingleThreadExecutor(), clients.
            getDefaultClient().
            configMaps().
            inAnyNamespace().
            withLabel( "managedBy", "ec-operator" ) );
    }
}
