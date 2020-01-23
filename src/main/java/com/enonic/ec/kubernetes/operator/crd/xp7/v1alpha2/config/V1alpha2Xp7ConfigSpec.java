package com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.config;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7ConfigSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7ConfigSpec.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7ConfigSpec.ExceptionMissing.class)
public abstract class V1alpha2Xp7ConfigSpec
    extends Configuration
{
    public abstract String file();

    public abstract String nodeGroup();

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
