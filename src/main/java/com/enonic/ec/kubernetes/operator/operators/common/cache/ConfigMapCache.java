package com.enonic.ec.kubernetes.operator.operators.common.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;

import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;

@Singleton
public class ConfigMapCache
    extends Cache<ConfigMap, ConfigMapList>
{
    @Inject
    public ConfigMapCache( Clients clients )
    {
        super( clients.
            getDefaultClient().
            configMaps().
            inAnyNamespace().
            withLabel( "managedBy", "ec-operator" ) );
    }
}
