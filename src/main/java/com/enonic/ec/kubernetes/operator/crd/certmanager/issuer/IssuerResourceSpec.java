package com.enonic.ec.kubernetes.operator.crd.certmanager.issuer;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableIssuerResourceSpec.Builder.class)
@Value.Immutable
public abstract class IssuerResourceSpec
{
    public abstract Object selfSigned();
}
