package com.enonic.cloud.operator.operators.common.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.config.V1alpha2Xp7Config;
import com.enonic.cloud.operator.operators.common.clients.Clients;
import com.enonic.cloud.operator.operators.common.clients.V1alpha2Xp7ConfigList;

@Singleton
public class ConfigCache
    extends Cache<V1alpha2Xp7Config, V1alpha2Xp7ConfigList>
{
    @Inject
    public ConfigCache( Clients clients )
    {
        super( clients.getConfigClient().inAnyNamespace() );
    }
}
