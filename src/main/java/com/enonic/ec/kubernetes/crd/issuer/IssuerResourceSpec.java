package com.enonic.ec.kubernetes.crd.issuer;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableIssuerResourceSpec.Builder.class)
@Value.Immutable
public abstract class IssuerResourceSpec
{
    public abstract Map<String, String> selfSigned();
}
