package com.enonic.ec.kubernetes.operator.crd.config.spec;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = Spec.ExceptionMissing.class, throwForNullPointer = Spec.ExceptionMissing.class)
public abstract class Spec
    extends Configuration
{
    public abstract String file();

    @Nullable
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
