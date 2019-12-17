package com.enonic.ec.kubernetes.operator.crd.xp7vhost.spec;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableXp7VHostSpec.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = Xp7VHostSpec.ExceptionMissing.class, throwForNullPointer = Xp7VHostSpec.ExceptionMissing.class)
public abstract class Xp7VHostSpec
    extends Configuration
{
    public abstract String host();

    @Value.Default
    public Boolean skipIngress()
    {
        return false;
    }

    @Nullable
    public abstract Xp7VHostSpecCertificate certificate();

    public abstract List<Xp7VHostSpecMapping> mappings();

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( mappings().size() > 0, "Field 'spec.mappings' has to contain more than 0 mappings" );
        if ( skipIngress() )
        {
            Preconditions.checkState( certificate() == null, "Field 'spec.certificate' cannot be set if ingress is skipped" );
        }
        mappings().stream().collect( Collectors.groupingBy( Xp7VHostSpecMapping::source ) ).entrySet().
            forEach( e -> Preconditions.checkState( e.getValue().size() == 1, "Field 'spec.mappings.target' has to be unique" ) );
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
