package com.enonic.ec.kubernetes.operator.crd.vhost.spec;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import com.enonic.ec.kubernetes.operator.crd.BuilderException;

@JsonDeserialize(builder = ImmutableSpecMapping.Builder.class)
@Value.Immutable
@Value.Style(throwForInvalidImmutableState = SpecMapping.ExceptionMissing.class, throwForNullPointer = SpecMapping.ExceptionMissing.class)
public abstract class SpecMapping
{
    public abstract String node();

    public abstract String source();

    public abstract String target();

    public String name( String host )
    {
        return Hashing.sha512().hashString( host + source(), Charsets.UTF_8 ).toString().substring( 0, 10 );
    }

    @Nullable
    public abstract String idProvider();

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
            return "spec.mappings";
        }
    }
}
