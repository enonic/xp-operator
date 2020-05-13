package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.Quantity;

import com.enonic.cloud.kubernetes.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7DeploymentSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7DeploymentSpec.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7DeploymentSpec.ExceptionMissing.class)
public abstract class V1alpha2Xp7DeploymentSpec
{
    public abstract String xpVersion();

    public abstract Boolean enabled();

    public abstract Map<String, Quantity> nodesSharedDisks();

    public abstract Map<String, V1alpha2Xp7DeploymentSpecNode> nodeGroups();

    @JsonIgnore
    @Value.Derived
    public int totalMasterNodes()
    {
        return nodeGroups().values().stream().filter( V1alpha2Xp7DeploymentSpecNode::master ).map(
            V1alpha2Xp7DeploymentSpecNode::replicas ).reduce( Integer::sum ).orElse( 0 );
    }

    @JsonIgnore
    @Value.Derived
    public int totalDataNodes()
    {
        return nodeGroups().values().stream().filter( V1alpha2Xp7DeploymentSpecNode::data ).map(
            V1alpha2Xp7DeploymentSpecNode::replicas ).reduce( Integer::sum ).orElse( 0 );
    }

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( nodeGroups().size() > 0, "Field 'spec.nodeGroups' has to contain more than 0 nodes" );
        Preconditions.checkState( totalMasterNodes() > 0, "Some nodes must have master=true" );
        Preconditions.checkState( totalMasterNodes() % 2 == 1, "Number of master nodes has to be an odd number" );
        Preconditions.checkState( totalDataNodes() > 0, "Some nodes must have data=true" );
    }

    public static class ExceptionMissing
        extends BuilderException
    {

        public ExceptionMissing( final String... missingAttributes )
        {
            super( missingAttributes );
        }

        @Override
        protected String getFieldPath()
        {
            return "spec";
        }
    }
}
