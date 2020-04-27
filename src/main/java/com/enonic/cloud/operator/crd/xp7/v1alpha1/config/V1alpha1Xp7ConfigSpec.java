package com.enonic.cloud.operator.crd.xp7.v1alpha1.config;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.cloud.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha1Xp7ConfigSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha1Xp7ConfigSpec.ExceptionMissing.class, throwForNullPointer = V1alpha1Xp7ConfigSpec.ExceptionMissing.class)
public abstract class V1alpha1Xp7ConfigSpec
{
    public abstract String file();

    public abstract String node();

    public abstract String data();

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
