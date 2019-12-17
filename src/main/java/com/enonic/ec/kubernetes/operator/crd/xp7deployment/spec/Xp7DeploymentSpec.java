package com.enonic.ec.kubernetes.operator.crd.xp7deployment.spec;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.Quantity;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableXp7DeploymentSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = Xp7DeploymentSpec.ExceptionMissing.class, throwForNullPointer = Xp7DeploymentSpec.ExceptionMissing.class)
public abstract class Xp7DeploymentSpec
{
    public abstract String xpVersion();

    public abstract Boolean enabled();

    public abstract Quantity nodesSharedDisk();

    public abstract Map<String, String> nodesSharedConfig();

    public abstract Map<String, Xp7DeploymentSpecNode> nodes();

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( nodes().size() > 0, "Field 'spec.nodes' has to contain more than 0 nodes" );

        Preconditions.checkState( nodes().values().stream().filter( Xp7DeploymentSpecNode::isMasterNode ).count() == 1,
                                  "1 and only 1 node has be of type " + Xp7DeploymentSpecNode.Type.MASTER );
        Preconditions.checkState( nodes().values().stream().filter( Xp7DeploymentSpecNode::isDataNode ).count() == 1,
                                  "1 and only 1 node has be of type " + Xp7DeploymentSpecNode.Type.DATA );
    }

    @Value.Default
    @JsonIgnore
    public boolean isClustered()
    {
        return nodes().values().stream().mapToInt( Xp7DeploymentSpecNode::replicas ).sum() > 1;
    } // TODO: REMOVE

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
