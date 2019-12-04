package com.enonic.ec.kubernetes.operator.crd.vhost.spec;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableSpecCertificate.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = SpecCertificate.ExceptionMissing.class, throwForNullPointer = SpecCertificate.ExceptionMissing.class)
public abstract class SpecCertificate
{
    @Nullable
    public abstract Boolean selfSigned();

    @Nullable
    public abstract SpecCertificateLetEncryptType letsEncrypt();

    @Value.Check
    protected void check()
    {
        boolean selfSigned = selfSigned() != null && selfSigned();
        boolean letsEncrypt = letsEncrypt() != null;
        Preconditions.checkState( selfSigned || letsEncrypt,
                                  "Some fields in 'spec.certificate' have to be set: [selfSigned or letsEncrypt]" );
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
