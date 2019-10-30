package com.enonic.ec.kubernetes.deployment.xpdeployment.spec;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableSpecVHostCertificate.Builder.class)
@Value.Immutable
public abstract class SpecVHostCertificate
{
    public abstract Boolean selfSigned();
}
