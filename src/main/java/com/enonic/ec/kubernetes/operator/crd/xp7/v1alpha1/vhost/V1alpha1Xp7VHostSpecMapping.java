package com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha1.vhost;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha1Xp7VHostSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha1Xp7VHostSpecMapping.ExceptionMissing.class, throwForNullPointer = V1alpha1Xp7VHostSpecMapping.ExceptionMissing.class)
public abstract class V1alpha1Xp7VHostSpecMapping
{
    public abstract String node();

    public abstract String source();

    public abstract String target();

    @Nullable
    public abstract String idProvider();

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
