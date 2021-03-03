package com.enonic.kubernetes.apis.xp.service;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;


@Value.Immutable
public abstract class AppEvent
{
    public abstract String namespace();

    public abstract String nodeGroup();

    public abstract AppEventType type();

    public abstract String key();

    @Nullable
    public abstract AppInfo info();
}
