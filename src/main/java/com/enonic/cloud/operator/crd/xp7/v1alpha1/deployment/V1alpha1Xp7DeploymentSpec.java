package com.enonic.cloud.operator.crd.xp7.v1alpha1.deployment;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.Quantity;

import com.enonic.cloud.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha1Xp7DeploymentSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha1Xp7DeploymentSpec.ExceptionMissing.class, throwForNullPointer = V1alpha1Xp7DeploymentSpec.ExceptionMissing.class)
public abstract class V1alpha1Xp7DeploymentSpec
{
    public abstract String xpVersion();

    public abstract Boolean enabled();

    public abstract Quantity nodesSharedDisk();

    public abstract Map<String, String> nodesSharedConfig();

    public abstract Map<String, V1alpha1Xp7DeploymentSpecNode> nodes();

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( nodes().size() > 0, "Field 'spec.nodes' has to contain more than 0 nodes" );

        Preconditions.checkState( nodes().values().stream().filter( V1alpha1Xp7DeploymentSpecNode::isMasterNode ).count() == 1,
                                  "1 and only 1 node has be of type " + V1alpha1Xp7DeploymentSpecNode.Type.MASTER );
        Preconditions.checkState( nodes().values().stream().filter( V1alpha1Xp7DeploymentSpecNode::isDataNode ).count() == 1,
                                  "1 and only 1 node has be of type " + V1alpha1Xp7DeploymentSpecNode.Type.DATA );
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
