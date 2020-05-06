package com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.cloud.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7DeploymentStatusFieldsPod.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7DeploymentStatusFieldsPod.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7DeploymentStatusFieldsPod.ExceptionMissing.class)
public abstract class V1alpha2Xp7DeploymentStatusFieldsPod
{
    public abstract String phase();

    public abstract Boolean ready();

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
            return "status.fields.pods";
        }
    }
}
