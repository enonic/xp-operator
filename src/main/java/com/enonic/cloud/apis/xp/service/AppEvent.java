package com.enonic.cloud.apis.xp.service;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;


@Value.Immutable
public abstract class AppEvent
{
    @Nullable
    public abstract AppInfo info();

    @Nullable
    public abstract AppKey uninstall();
}
