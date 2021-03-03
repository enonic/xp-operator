package com.enonic.kubernetes.apis.xp.service;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableAppInstallResponse.Builder.class)
@Value.Immutable
public abstract class AppInstallResponse
{
    @Nullable
    public abstract String failure();

    @Nullable
    public abstract AppInstallJson applicationInstalledJson();
}
