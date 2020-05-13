package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.cloud.kubernetes.crd.BuilderException;
import com.enonic.cloud.kubernetes.crd.status.CrdStatus;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7VHostStatus.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7VHostStatus.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7VHostStatus.ExceptionMissing.class)
public abstract class V1alpha2Xp7VHostStatus
    extends CrdStatus<V1alpha2Xp7VHostStatusFields>
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
