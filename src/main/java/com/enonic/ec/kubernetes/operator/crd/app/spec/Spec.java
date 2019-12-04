package com.enonic.ec.kubernetes.operator.crd.app.spec;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = Spec.ExceptionMissing.class, throwForNullPointer = Spec.ExceptionMissing.class)
public abstract class Spec
{
    public abstract String uri();

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
