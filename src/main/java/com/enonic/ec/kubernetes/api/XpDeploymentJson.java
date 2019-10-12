package com.enonic.ec.kubernetes.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;

import com.enonic.ec.kubernetes.deployment.XpDeployment.XpDeploymentResourceSpec;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

@JsonDeserialize(builder = XpDeploymentJson.Builder.class)
@RegisterForReflection
public class XpDeploymentJson
{
    private final String uid;

    private final String apiVersion;

    private final XpDeploymentResourceSpec spec;

    private XpDeploymentJson( final Builder builder )
    {
        uid = builder.uid;
        apiVersion = assertNotNull( "apiVersion", builder.apiVersion );
        spec = assertNotNull( "spec", builder.spec );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public String getUid()
    {
        return uid;
    }

    public XpDeploymentResourceSpec getSpec()
    {
        return spec;
    }

    public String getApiVersion()
    {
        return apiVersion;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder
    {
        private String uid;

        private String apiVersion;

        private XpDeploymentResourceSpec spec;

        private Builder()
        {
        }

        public Builder uid( final String val )
        {
            uid = val;
            return this;
        }

        public Builder apiVersion( final String val )
        {
            apiVersion = val;
            return this;
        }

        public Builder spec( final XpDeploymentResourceSpec val )
        {
            spec = val;
            return this;
        }

        public XpDeploymentJson build()
        {
            return new XpDeploymentJson( this );
        }
    }
}
