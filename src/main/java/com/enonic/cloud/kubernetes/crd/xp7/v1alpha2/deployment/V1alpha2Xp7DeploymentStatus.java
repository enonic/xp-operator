package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.deployment;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.cloud.kubernetes.crd.BuilderException;
import com.enonic.cloud.kubernetes.crd.status.CrdStatus;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7DeploymentStatus.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7DeploymentStatus.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7DeploymentStatus.ExceptionMissing.class)
public abstract class V1alpha2Xp7DeploymentStatus
    extends CrdStatus<V1alpha2Xp7DeploymentStatusFields>
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
