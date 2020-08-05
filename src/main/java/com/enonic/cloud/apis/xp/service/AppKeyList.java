package com.enonic.cloud.apis.xp.service;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableAppKeyList.Builder.class)
@Value.Immutable
public abstract class AppKeyList
{
    public abstract List<String> key();
}
