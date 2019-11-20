package com.enonic.ec.kubernetes.crd.vhost.spec;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.crd.BuilderException;

@JsonDeserialize(builder = ImmutableSpecCertificate.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = SpecCertificate.ExceptionMissing.class, throwForNullPointer = SpecCertificate.ExceptionMissing.class)
public abstract class SpecCertificate
{
    public abstract Boolean selfSigned();

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
            return "spec.certificate";
        }
    }
}
