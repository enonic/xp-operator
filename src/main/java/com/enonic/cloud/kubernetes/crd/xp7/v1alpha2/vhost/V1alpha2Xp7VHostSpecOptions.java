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
    public Boolean ingress()
    {
        return true;
    }

    @Value.Default
    public String ingressMaxBodySize()
    {
        return "100m";
    }

    @Value.Default
    public Boolean dnsRecord()
    {
        return ingress();
    }

    @Value.Default
    public Boolean statusCake()
    {
        return false;
    }

    @Value.Default
    public Boolean caching()
    {
        return false;
    }

    @Value.Default
    public Boolean cdn()
    {
        return false;
    }

    @Value.Default
    public String ipWhitelist()
    {
        return "";
    }

    @Value.Check
    protected void check()
    {
        if ( !ingress() )
        {
            Preconditions.checkState( !dnsRecord(), "Field 'spec.options.dnsRecord' cannot be true if 'spec.options.ingress' is false" );
        }

        if ( !dnsRecord() )
        {
            Preconditions.checkState( !statusCake(),
                                      "Field 'spec.options.statusCake' cannot be true if 'spec.options.dnsRecord' is false" );
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
            return "spec";
        }
    }
}
