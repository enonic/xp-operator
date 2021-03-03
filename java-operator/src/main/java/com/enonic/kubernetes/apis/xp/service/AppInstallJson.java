package com.enonic.kubernetes.apis.xp.service;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableAppInstallJson.Builder.class)
@Value.Immutable
public abstract class AppInstallJson
{
    public abstract AppInfo application();
}
