package com.enonic.cloud.apis.doh.service;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


@JsonDeserialize(builder = ImmutableDohResponse.Builder.class)
@Value.Immutable
public abstract class DohResponse
{
    @JsonProperty("Status")
    public abstract Integer status();

    @JsonProperty("TC")
    public abstract Boolean truncated();

    @JsonProperty("RD")
    public abstract Boolean recursionDesired();

    @JsonProperty("RA")
    public abstract Boolean recursionAvailable();

    @JsonProperty("AD")
    public abstract String authenticatedData();

    @JsonProperty("CD")
    public abstract String checkingDisabled();

    @JsonProperty("Question")
    public abstract List<DohQuestion> questions();

    @JsonProperty("Answer")
    public abstract List<DohAnswer> answers();

    @JsonProperty("Authority")
    public abstract List<DohAnswer> authorities();
}
