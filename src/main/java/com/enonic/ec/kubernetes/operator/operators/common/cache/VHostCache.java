package com.enonic.ec.kubernetes.operator.operators.common.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.ec.kubernetes.operator.operators.common.clients.Clients;
import com.enonic.ec.kubernetes.operator.operators.common.clients.V1alpha2Xp7VHostList;

@Singleton
public class VHostCache
    extends Cache<V1alpha2Xp7VHost, V1alpha2Xp7VHostList>
{
    @Inject
    public VHostCache( Clients clients )
    {
        super( clients.
            getVHostClient().
            inAnyNamespace() );
    }
}
