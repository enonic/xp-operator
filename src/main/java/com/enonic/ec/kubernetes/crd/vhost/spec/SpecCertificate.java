package com.enonic.ec.kubernetes.crd.vhost.spec;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.crd.BuilderException;

@JsonDeserialize(builder = ImmutableSpecCertificate.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = SpecCertificate.ExceptionMissing.class, throwForNullPointer = SpecCertificate.ExceptionMissing.class)
public abstract class SpecCertificate
{
    @Value.Default
    public Boolean selfSigned()
    {
        return false;
    }

    @Value.Default
    public Boolean letsEncrypt()
    {
        return false;
    }

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( selfSigned() || letsEncrypt(),
                                  "Some fields in 'spec.certificate' have to be set to true: [selfSigned or letsEncrypt]" );
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
            return "spec.certificate";
        }
    }
}
