package com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.cloud.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7VHostSpecMapping.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7VHostSpecMapping.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7VHostSpecMapping.ExceptionMissing.class)
public abstract class V1alpha2Xp7VHostSpecMapping
{
    public abstract String nodeGroup();

    public abstract String source();

    public abstract String target();

    @Nullable
    public abstract V1alpha2Xp7VHostSpecMappingIdProviders idProviders();

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
            return "spec.mappings";
        }
    }
}