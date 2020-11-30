package com.enonic.cloud.apis.xp.service;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableAppEventList.Builder.class)
@Value.Immutable
public abstract class AppEventList
{
    public abstract List<AppInfo> applications();
}
