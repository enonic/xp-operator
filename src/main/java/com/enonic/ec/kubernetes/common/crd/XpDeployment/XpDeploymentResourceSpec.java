package com.enonic.ec.kubernetes.common.crd.XpDeployment;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;

import static com.enonic.ec.kubernetes.common.assertions.Assertions.assertNotNull;

@JsonDeserialize(builder = XpDeploymentResourceSpec.Builder.class)
@RegisterForReflection
public class XpDeploymentResourceSpec
{
    private final String xpVersion;

    private final String cloud;

    private final String project;

    private final String app;

    private final String name;

    private final Map<String, String> config;

    private XpDeploymentResourceSpec( final Builder builder )
    {
        xpVersion = assertNotNull( "xpVersion", builder.xpVersion );
        cloud = assertNotNull( "cloud", builder.cloud );
        project = assertNotNull( "project", builder.project );
        app = assertNotNull( "app", builder.app );
        name = assertNotNull( "name", builder.name );
        config = assertNotNull( "config", builder.config );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public String getXpVersion()
    {
        return xpVersion;
    }

    public String getCloud()
    {
        return cloud;
    }

    public String getProject()
    {
        return project;
    }

    public String getApp()
    {
        return app;
    }

    public String getName()
    {
        return name;
    }

    public Map<String, String> getConfig()
    {
        return config;
    }

    @JsonIgnore
    public String getFullProjectName()
    {
        return String.join( "-", getCloud(), getProject() );
    }

    @JsonIgnore
    public String getFullAppName()
    {
        return String.join( "-", getApp(), getName() );
    }

    @JsonIgnore
    public String getFullName()
    {
        return String.join( "-", getFullProjectName(), getFullAppName() );
    }

    @JsonIgnore
    public Map<String, String> getDefaultLabels()
    {
        return Map.of( "cloud", getCloud(), "project", getProject(), "app", getApp(), "name", getName() );
    }

    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
    public static final class Builder
    {
        private String xpVersion;

        private String cloud;

        private String project;

        private String app = "xp";

        private String name;

        private Map<String, String> config;

        private Builder()
        {
        }

        public Builder xpVersion( final String val )
        {
            xpVersion = val;
            return this;
        }

        public Builder cloud( final String val )
        {
            cloud = val;
            return this;
        }

        public Builder project( final String val )
        {
            project = val;
            return this;
        }

        public Builder app( final String val )
        {
            app = val;
            return this;
        }

        public Builder name( final String val )
        {
            name = val;
            return this;
        }

        public Builder config( final Map<String, String> val )
        {
            config = val;
            return this;
        }

        public XpDeploymentResourceSpec build()
        {
            return new XpDeploymentResourceSpec( this );
        }
    }
}
