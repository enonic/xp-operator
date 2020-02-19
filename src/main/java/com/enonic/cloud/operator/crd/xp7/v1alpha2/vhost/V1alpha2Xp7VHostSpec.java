package com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.cloud.operator.common.Configuration;
import com.enonic.cloud.operator.common.Validator;
import com.enonic.cloud.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableV1alpha2Xp7VHostSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7VHostSpec.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7VHostSpec.ExceptionMissing.class)
public abstract class V1alpha2Xp7VHostSpec
    extends Configuration
{
    public abstract String host();

    @Value.Default
    public String maxBodySize()
    {
        return "100m";
    }

    @Value.Default
    public Boolean skipIngress()
    {
        return false;
    }

    @Nullable
    public abstract V1alpha2Xp7VHostSpecCertificate certificate();

    public abstract List<V1alpha2Xp7VHostSpecMapping> mappings();

    @SuppressWarnings("DuplicatedCode")
    @Value.Check
    protected void check()
    {
        Validator.dns1123( "spec.host", host() );
        Preconditions.checkState( mappings().size() > 0, "Field 'spec.mappings' has to contain more than 0 mappings" );
        if ( skipIngress() )
        {
            Preconditions.checkState( certificate() == null, "Field 'spec.certificate' cannot be set if ingress is skipped" );
        }
        mappings().stream().collect( Collectors.groupingBy( V1alpha2Xp7VHostSpecMapping::source ) ).forEach(
            ( key, value ) -> Preconditions.checkState( value.size() == 1, "Field 'spec.mappings.target' has to be unique" ) );
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
