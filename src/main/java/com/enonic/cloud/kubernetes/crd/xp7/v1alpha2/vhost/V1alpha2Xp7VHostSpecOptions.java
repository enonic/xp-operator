package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.cloud.kubernetes.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7VHostSpecOptions.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7VHostSpecOptions.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7VHostSpecOptions.ExceptionMissing.class)
public abstract class V1alpha2Xp7VHostSpecOptions
{
    @Value.Default
    public Boolean dnsRecord()
    {
        return true;
    }

    @Value.Default
    public Boolean cdn()
    {
        return true;
    }

    @Value.Check
    protected void check()
    {
        if ( !dnsRecord() )
        {
            Preconditions.checkState( !cdn(), "Field 'spec.options.cdn' cannot be true if 'spec.options.dnsRecord' is false" );
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
            return "spec.options";
        }
    }
}
