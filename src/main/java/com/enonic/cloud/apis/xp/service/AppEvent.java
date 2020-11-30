package com.enonic.cloud.apis.xp.service;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import static com.enonic.cloud.common.Configuration.cfgStr;


@Value.Immutable
public abstract class AppEvent
{
    public abstract String namespace();

    @Value.Default
    public String nodeGroup()
    {
        return cfgStr( "operator.charts.values.allNodesKey" );
    }

    @Nullable
    public abstract AppInfo info();

    @Nullable
    public abstract AppKey uninstall();
}
