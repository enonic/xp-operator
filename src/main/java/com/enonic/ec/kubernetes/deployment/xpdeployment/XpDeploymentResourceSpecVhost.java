package com.enonic.ec.kubernetes.deployment.xpdeployment;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableXpDeploymentResourceSpecVhost.Builder.class)
@Value.Immutable
public abstract class XpDeploymentResourceSpecVhost
{
    public abstract String host();

    public abstract String config();

    public abstract XpDeploymentResourceSpecVhostCertificate certificate();
}
