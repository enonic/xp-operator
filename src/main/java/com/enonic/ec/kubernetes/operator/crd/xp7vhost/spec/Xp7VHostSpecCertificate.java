package com.enonic.ec.kubernetes.operator.crd.xp7vhost.spec;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableXp7VHostSpecCertificate.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = Xp7VHostSpecCertificate.ExceptionMissing.class, throwForNullPointer = Xp7VHostSpecCertificate.ExceptionMissing.class)
public abstract class Xp7VHostSpecCertificate
{
    public abstract Xp7VHostSpecCertificateAuthority authority();

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
