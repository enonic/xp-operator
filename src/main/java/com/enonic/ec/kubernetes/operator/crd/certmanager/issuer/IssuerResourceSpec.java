package com.enonic.ec.kubernetes.operator.crd.certmanager.issuer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize(builder = IssuerResourceSpec.Builder.class)
@RegisterForReflection
public class IssuerResourceSpec
{
    private Object selfSigned;

    private IssuerResourceSpec( final Builder builder )
    {
        selfSigned = builder.selfSigned;
    }

    public Object getSelfSigned()
    {
        return selfSigned;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder
    {
        private Object selfSigned;

        private Builder()
        {
        }

        public Builder selfSigned( final Object val )
        {
            selfSigned = val;
            return this;
        }

        public IssuerResourceSpec build()
        {
            return new IssuerResourceSpec( this );
        }
    }
}
