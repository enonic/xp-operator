package com.enonic.ec.kubernetes.crd.vhost.spec;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableSpecCertificate.Builder.class)
@Value.Immutable
public abstract class SpecCertificate
{
    public abstract Boolean selfSigned();
}
