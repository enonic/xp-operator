package com.enonic.ec.kubernetes.operator.crd.xp7app.spec;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableXp7AppSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = Xp7AppSpec.ExceptionMissing.class, throwForNullPointer = Xp7AppSpec.ExceptionMissing.class)
public abstract class Xp7AppSpec
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
