package com.enonic.cloud.operator.operators.common.cache;

import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;

import com.enonic.cloud.operator.operators.common.clients.Clients;

import static com.enonic.cloud.operator.common.Configuration.cfgStr;

@Singleton
public class PodCache
    extends Cache<Pod, PodList>
{
    @Inject
    public PodCache( Clients clients )
    {
        super( Executors.newSingleThreadExecutor(), clients.
            getDefaultClient().
            pods().
            inAnyNamespace().
            withLabel( cfgStr( "operator.helm.charts.Values.labelKeys.managed" ), "true" ) );
    }
}
