package com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment;

import java.util.Map;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.cloud.operator.common.Configuration;
import com.enonic.cloud.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7DeploymentSpecNode.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7DeploymentSpecNode.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7DeploymentSpecNode.ExceptionMissing.class)
public abstract class V1alpha2Xp7DeploymentSpecNode
    extends Configuration
{
    @Nullable
    public abstract String displayName();

    public abstract boolean master();

    public abstract boolean data();

    public abstract Integer replicas();

    public abstract V1alpha2Xp7DeploymentSpecNodeResources resources();

    public abstract Map<String, String> env();

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( replicas() > 0, "Field 'spec.nodes.replicas' has to be > 0" );

        Preconditions.checkState( !data() || resources().disks().containsKey( "index" ),
                                  "Nodes with data=true must have disk 'index' defined" );
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
            return "spec.nodes";
        }
    }
}
