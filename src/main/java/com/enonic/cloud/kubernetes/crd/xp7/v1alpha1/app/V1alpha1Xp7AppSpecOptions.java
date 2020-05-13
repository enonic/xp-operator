package com.enonic.cloud.kubernetes.crd.xp7.v1alpha1.app;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.cloud.kubernetes.crd.BuilderException;


@JsonDeserialize(builder = ImmutableV1alpha1Xp7AppSpecOptions.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha1Xp7AppSpecOptions.ExceptionMissing.class, throwForNullPointer = V1alpha1Xp7AppSpecOptions.ExceptionMissing.class)
public abstract class V1alpha1Xp7AppSpecOptions
{
    @Value.Default
    public Boolean bootstrap()
    {
        return false;
    }

    @Nullable
    public abstract String appKey();

    @Value.Check
    protected void check()
    {
        if ( bootstrap() )
        {
            Preconditions.checkState( appKey() != null, "'spec.options.appKey' cannot be null when 'spec.options.bootstrap' is true" );
        }
    }

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
            return "spec.options";
        }
    }
}
