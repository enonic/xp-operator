package com.enonic.ec.kubernetes.deployment.xpdeployment;

import java.util.Map;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ImmutableXpDeploymentResourceSpecNode.Builder.class)
@Value.Immutable
public abstract class XpDeploymentResourceSpecNode
{
    public abstract String alias();

    public abstract Integer replicas();

    public abstract NodeType type();

    public abstract XpDeploymentResourceSpecNodeResources resources();

    @Nullable
    public abstract Map<String, String> env();

    public abstract Map<String, String> config();

    @JsonIgnore
    @Value.Derived
    public Map<String, String> extraLabels()
    {
        return Map.of( "xp.node.alias." + alias(), alias() );
    }

}
