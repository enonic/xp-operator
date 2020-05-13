package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.cloud.kubernetes.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7VHostSpecCertificate.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7VHostSpecCertificate.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7VHostSpecCertificate.ExceptionMissing.class)
public abstract class V1alpha2Xp7VHostSpecCertificate
{
    public abstract V1alpha2Xp7VHostSpecCertificateAuthority authority();

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
