package com.enonic.cloud.apis.xp.service;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableAppInfo.Builder.class)
@Value.Immutable
public abstract class AppEventList
{
    public abstract List<AppInfo> applications();
}
