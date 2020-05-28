package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.cloud.kubernetes.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7VHostSpecCertificate.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7VHostSpecCertificate.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7VHostSpecCertificate.ExceptionMissing.class)
public abstract class V1alpha2Xp7VHostSpecCertificate
{
    public abstract V1alpha2Xp7VHostSpecCertificateAuthority authority();

    @Nullable
    public abstract String identifier();

    @Value.Check
    protected void check()
    {
        if (authority() == V1alpha2Xp7VHostSpecCertificateAuthority.SECRET) {
            Preconditions.checkState( identifier() != null, "Field 'spec.certificate.identifier' cannot be null when 'spec.certificate.authority' is 'secret'" );
        }
        if (authority() == V1alpha2Xp7VHostSpecCertificateAuthority.ISSUER) {
            Preconditions.checkState( identifier() != null, "Field 'spec.certificate.identifier' cannot be null when 'spec.certificate.authority' is 'issuer'" );
        }

        if (identifier() != null) {
            Preconditions.checkState( authority() == V1alpha2Xp7VHostSpecCertificateAuthority.SECRET || authority() == V1alpha2Xp7VHostSpecCertificateAuthority.ISSUER, "Field 'spec.certificate.authority' has to be 'secret' or 'issuer' if field 'spec.certificate.secretName' is provided");
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
            return "spec.certificate";
        }
    }
}
