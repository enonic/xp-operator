package com.enonic.ec.kubernetes.operator.crd.xp7config.spec;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableXp7ConfigSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = Xp7ConfigSpec.ExceptionMissing.class, throwForNullPointer = Xp7ConfigSpec.ExceptionMissing.class)
public abstract class Xp7ConfigSpec
    extends Configuration
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
