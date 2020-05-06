package com.enonic.cloud.operator.crd;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

@Value.Style(throwForInvalidImmutableState = CrdStatus.ExceptionMissing.class, throwForNullPointer = CrdStatus.ExceptionMissing.class)
public abstract class CrdStatus<F>
{
    @Value.Default
    public CrdStatusState state()
    {
        return CrdStatusState.PENDING;
    }

    @Nullable
    public abstract String message();

    @Nullable
    public abstract F fields();

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