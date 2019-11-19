package com.enonic.ec.kubernetes.crd.deployment.spec;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.Quantity;

@JsonDeserialize(builder = ImmutableSpec.Builder.class)
@Value.Immutable
public abstract class Spec
{
    public abstract String xpVersion();

    public abstract Boolean enabled();

    public abstract Quantity sharedDisk();

    public abstract Map<String, SpecNode> nodes();

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( nodes().size() > 0, "field 'nodes' has to contain more than 0 nodes" );

        Preconditions.checkState( nodes().values().stream().filter( SpecNode::isMasterNode ).count() == 1,
                                  "1 and only 1 node has be of type master" );
        Preconditions.checkState( nodes().values().stream().filter( SpecNode::isDataNode ).count() == 1,
                                  "1 and only node has be of type data" );
    }

    @Value.Default
    @JsonIgnore
    public boolean isClustered()
    {
        return nodes().values().stream().mapToInt( SpecNode::replicas ).sum() > 1;
    } // TODO: REMOVE
}
