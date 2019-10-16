package com.enonic.ec.kubernetes.deployment.xpdeployment;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableXpDeploymentResourceSpecVhostCertificate.Builder.class)
@Value.Immutable
public abstract class XpDeploymentResourceSpecVhostCertificate
{
    public abstract Boolean selfSigned();
}
