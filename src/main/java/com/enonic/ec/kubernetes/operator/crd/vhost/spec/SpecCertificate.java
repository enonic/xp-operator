package com.enonic.ec.kubernetes.operator.crd.vhost.spec;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableSpecCertificate.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = SpecCertificate.ExceptionMissing.class, throwForNullPointer = SpecCertificate.ExceptionMissing.class)
public abstract class SpecCertificate
{
    public abstract SpecCertificateAuthority authority();

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
