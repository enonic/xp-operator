package com.enonic.cloud.operator.crd.xp7.v1alpha1.deployment;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.Quantity;

import com.enonic.cloud.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha1Xp7DeploymentSpecNode.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha1Xp7DeploymentSpecNodeResources.ExceptionMissing.class, throwForNullPointer = V1alpha1Xp7DeploymentSpecNodeResources.ExceptionMissing.class)
public abstract class V1alpha1Xp7DeploymentSpecNodeResources
{
    public abstract Quantity cpu();

    public abstract Quantity memory();

    public abstract Map<String, Quantity> disks();

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
            return "spec.nodes.resources";
        }
    }
}
