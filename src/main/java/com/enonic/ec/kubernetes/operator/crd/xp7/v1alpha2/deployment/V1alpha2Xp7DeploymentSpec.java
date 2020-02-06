package com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.Quantity;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7DeploymentSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7DeploymentSpec.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7DeploymentSpec.ExceptionMissing.class)
public abstract class V1alpha2Xp7DeploymentSpec
{
    public abstract String xpVersion();

    public abstract Boolean enabled();

    public abstract Map<String, Quantity> nodesSharedDisks();

    public abstract Map<String, V1alpha2Xp7DeploymentSpecNode> nodeGroups();

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( nodeGroups().size() > 0, "Field 'spec.nodes' has to contain more than 0 nodes" );

        Preconditions.checkState( nodeGroups().values().stream().filter( V1alpha2Xp7DeploymentSpecNode::master ).count() == 1,
                                  "1 and only 1 node has be of master=true" );
        Preconditions.checkState( nodeGroups().values().stream().filter( V1alpha2Xp7DeploymentSpecNode::data ).count() == 1,
                                  "1 and only 1 node has be of type data=true" );
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
