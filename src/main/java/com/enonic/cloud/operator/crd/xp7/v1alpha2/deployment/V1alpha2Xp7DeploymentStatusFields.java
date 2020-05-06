package com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.cloud.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7DeploymentStatusFields.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7DeploymentStatusFields.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7DeploymentStatusFields.ExceptionMissing.class)
public abstract class V1alpha2Xp7DeploymentStatusFields
{
    public abstract Map<String, V1alpha2Xp7DeploymentStatusFieldsPod> pods();

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
