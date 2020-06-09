package com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.cloud.common.Validator;
import com.enonic.cloud.kubernetes.crd.BuilderException;


@JsonDeserialize(builder = ImmutableV1alpha2Xp7VHostSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = V1alpha2Xp7VHostSpec.ExceptionMissing.class, throwForNullPointer = V1alpha2Xp7VHostSpec.ExceptionMissing.class)
public abstract class V1alpha2Xp7VHostSpec
{
    public abstract String host();

    @Value.Default
    public V1alpha2Xp7VHostSpecOptions options()
    {
        return ImmutableV1alpha2Xp7VHostSpecOptions.builder().build();
    }

    @Nullable
    public abstract V1alpha2Xp7VHostSpecCertificate certificate();

    public abstract List<V1alpha2Xp7VHostSpecMapping> mappings();

    @JsonIgnore
    @Value.Derived
    public boolean hasIngress()
    {
        for ( V1alpha2Xp7VHostSpecMapping m : mappings() )
        {
            if ( m.options().ingress() )
            {
                return true;
            }
        }
        return false;
    }

    @Value.Check
    protected void check()
    {
        Validator.dns1123( "spec.host", host() );
        Preconditions.checkState( mappings().size() > 0, "Field 'spec.mappings' has to contain more than 0 mappings" );
        if ( !hasIngress() )
        {
            Preconditions.checkState( certificate() == null, "Field 'spec.certificate' cannot be set if ingress is skipped" );
        }
        mappings().stream().collect( Collectors.groupingBy( V1alpha2Xp7VHostSpecMapping::source ) ).forEach(
            ( key, value ) -> Preconditions.checkState( value.size() == 1, "Field 'spec.mappings.source' has to be unique" ) );
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
