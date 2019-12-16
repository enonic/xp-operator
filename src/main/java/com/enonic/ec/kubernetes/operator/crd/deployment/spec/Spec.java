package com.enonic.ec.kubernetes.operator.crd.deployment.spec;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.Quantity;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = Spec.ExceptionMissing.class, throwForNullPointer = Spec.ExceptionMissing.class)
public abstract class Spec
{
    public abstract String xpVersion();

    public abstract Boolean enabled();

    public abstract Quantity nodesSharedDisk();

    public abstract Map<String, String> nodesSharedConfig();

    public abstract Map<String, SpecNode> nodes();

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( nodes().size() > 0, "Field 'spec.nodes' has to contain more than 0 nodes" );

        Preconditions.checkState( nodes().values().stream().filter( SpecNode::isMasterNode ).count() == 1,
                                  "1 and only 1 node has be of type " + SpecNode.Type.MASTER );
        Preconditions.checkState( nodes().values().stream().filter( SpecNode::isDataNode ).count() == 1,
                                  "1 and only 1 node has be of type " + SpecNode.Type.DATA );
    }

    @Value.Default
    @JsonIgnore
    public boolean isClustered()
    {
        return nodes().values().stream().mapToInt( SpecNode::replicas ).sum() > 1;
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
