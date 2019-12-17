package com.enonic.ec.kubernetes.operator.crd.xp7deployment.spec;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.Quantity;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableXp7DeploymentSpecNodeResources.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = Xp7DeploymentSpecNodeResources.ExceptionMissing.class, throwForNullPointer = Xp7DeploymentSpecNodeResources.ExceptionMissing.class)
public abstract class Xp7DeploymentSpecNodeResources
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
