package com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.cloud.kubernetes.crd.BuilderException;
import com.enonic.cloud.kubernetes.crd.status.CrdStatus;

@JsonDeserialize(builder = ImmutableV1alpha1Xp7AppStatus.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha1Xp7AppStatus.ExceptionMissing.class, throwForNullPointer = V1alpha1Xp7AppStatus.ExceptionMissing.class)
public abstract class V1alpha1Xp7AppStatus
    extends CrdStatus<V1alpha1Xp7AppStatusFields>
{
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
            return "status";
        }
    }
}
