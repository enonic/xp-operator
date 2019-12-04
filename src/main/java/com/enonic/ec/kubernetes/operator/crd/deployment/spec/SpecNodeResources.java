package com.enonic.ec.kubernetes.operator.crd.deployment.spec;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.Quantity;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableSpecNodeResources.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = SpecNodeResources.ExceptionMissing.class, throwForNullPointer = SpecNodeResources.ExceptionMissing.class)
public abstract class SpecNodeResources
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
