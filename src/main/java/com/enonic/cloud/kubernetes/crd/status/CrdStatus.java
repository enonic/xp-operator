package com.enonic.cloud.kubernetes.crd.status;

import org.immutables.value.Value;

import com.enonic.cloud.kubernetes.crd.BuilderException;

@Value.Style(throwForInvalidImmutableState = CrdStatus.ExceptionMissing.class, throwForNullPointer = CrdStatus.ExceptionMissing.class)
public abstract class CrdStatus<F>
{
    @Value.Default
    public CrdStatusState state()
    {
        return CrdStatusState.PENDING;
    }

    @Value.Default
    public String message()
    {
        return "Pending";
    }

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