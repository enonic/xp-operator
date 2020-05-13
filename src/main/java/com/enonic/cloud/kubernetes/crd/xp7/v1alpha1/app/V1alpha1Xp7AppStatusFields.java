package com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.cloud.kubernetes.crd.BuilderException;


@JsonDeserialize(builder = ImmutableV1alpha1Xp7AppStatusFields.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha1Xp7AppStatusFields.ExceptionMissing.class, throwForNullPointer = V1alpha1Xp7AppStatusFields.ExceptionMissing.class)
public abstract class V1alpha1Xp7AppStatusFields
{
    @Nullable
    public abstract String appKey();

    @SuppressWarnings("WeakerAccess")
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
            return "status.fields";
        }
    }
}
