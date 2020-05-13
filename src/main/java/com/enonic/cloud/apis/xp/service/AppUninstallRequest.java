package com.enonic.cloud.apis.xp.service;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableAppUninstallRequest.Builder.class)
@Value.Immutable
public abstract class AppUninstallRequest
{
    public abstract List<String> key();
}
