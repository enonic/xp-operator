package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.cloud.kubernetes.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7VHostSpecMappingOptions.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7VHostSpecMappingOptions.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7VHostSpecMappingOptions.ExceptionMissing.class)
public abstract class V1alpha2Xp7VHostSpecMappingOptions
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
    public String ipWhitelist()
    {
        return "";
    }

    @Value.Default
    public Boolean stickySession()
    {
        return false;
    }

    @Value.Default
    public Boolean sslRedirect()
    {
        return true;
    }

    @Value.Check
    protected void check()
    {
        if ( !ingress() )
        {
            Preconditions.checkState( !statusCake(),
                                      "Field 'spec.mappings.options.statusCake' cannot be true if 'spec.mappings.options.ingress' is false" );
            Preconditions.checkState( !caching(),
                                      "Field 'spec.mappings.options.caching' cannot be true if 'spec.mappings.options.ingress' is false" );
            Preconditions.checkState( "".equals( ipWhitelist() ),
                                      "Field 'spec.mappings.options.ipWhitelist' cannot be true if 'spec.mappings.options.ingress' is false" );
            Preconditions.checkState( !stickySession(),
                                      "Field 'spec.mappings.options.stickySession' cannot be true if 'spec.mappings.options.ingress' is false" );
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
            return "spec.mappings.options";
        }
    }
}
