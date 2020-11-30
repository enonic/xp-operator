package com.enonic.cloud.apis.xp.service;

import java.util.List;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableAppInfo.Builder.class)
@Value.Immutable
public abstract class AppInfo
{
    @Nullable
    public abstract Object config();

    @Nullable
    public abstract String description();

    @Nullable
    public abstract String iconUrl();

    @Nullable
    public abstract Object idProviderConfig();

    @Nullable
    public abstract List<Object> metaSteps();

    @Nullable
    public abstract Boolean deletable();

    @Nullable
    public abstract String displayName();

    @Nullable
    public abstract Boolean editable();

    public abstract String key();

    public abstract boolean local();

    @Nullable
    public abstract String maxSystemVersion();

    @Nullable
    public abstract String minSystemVersion();

    @Nullable
    public abstract String modifiedTime();

    public abstract String state();

    @Nullable
    public abstract String url();

    @Nullable
    public abstract String vendorName();

    @Nullable
    public abstract String vendorUrl();

    @Nullable
    public abstract String version();
}
