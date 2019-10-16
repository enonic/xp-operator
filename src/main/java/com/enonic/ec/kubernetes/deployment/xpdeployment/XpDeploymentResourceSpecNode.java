package com.enonic.ec.kubernetes.deployment.xpdeployment;

import java.util.List;
import java.util.Map;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableXpDeploymentResourceSpecNode.Builder.class)
@Value.Immutable
public abstract class XpDeploymentResourceSpecNode
{
    public abstract Integer replicas();

    @Nullable
    public abstract List<String> vhost();

    public abstract NodeType type();

    public abstract XpDeploymentResourceSpecNodeResources resources();

    @Nullable
    public abstract Map<String, String> env();

    public abstract Map<String, String> config();

}
