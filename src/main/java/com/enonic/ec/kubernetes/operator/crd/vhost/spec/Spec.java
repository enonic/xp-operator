package com.enonic.ec.kubernetes.operator.crd.vhost.spec;

import java.util.List;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = Spec.ExceptionMissing.class, throwForNullPointer = Spec.ExceptionMissing.class)
public abstract class Spec
    extends Configuration
{
    public abstract String host();

    @Nullable
    public abstract SpecCertificate certificate();

    public abstract List<SpecMapping> mappings();

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
            return "spec";
        }
    }
}
