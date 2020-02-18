package com.enonic.cloud.operator.crd.xp7.v1alpha1.app;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.cloud.operator.crd.BuilderException;


@JsonDeserialize(builder = ImmutableV1alpha1Xp7AppSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha1Xp7AppSpec.ExceptionMissing.class, throwForNullPointer = V1alpha1Xp7AppSpec.ExceptionMissing.class)
public abstract class V1alpha1Xp7AppSpec
{
    public abstract String url();

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
