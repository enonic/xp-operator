package com.enonic.ec.kubernetes.deployment.xpdeployment;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.Quantity;

@JsonDeserialize(builder = ImmutableXpDeploymentResourceSpec.Builder.class)
@Value.Immutable
public abstract class XpDeploymentResourceSpec
{
    public abstract String xpVersion();

    public abstract String cloud();

    public abstract String project();

    @Value.Default
    public String app()
    {
        return "xp";
    }

    public abstract String name();

    public abstract Map<String, Quantity> resources();

    public abstract Map<String, String> config();

    @Value.Derived
    @JsonIgnore
    public String fullProjectName()
    {
        return String.join( "-", cloud(), project() );
    }

    @Value.Derived
    @JsonIgnore
    public String fullAppName()
    {
        return String.join( "-", app(), name() );
    }

    @Value.Derived
    @JsonIgnore
    public String fullName()
    {
        return String.join( "-", fullProjectName(), fullAppName() );
    }

    @Value.Derived
    @JsonIgnore
    public Map<String, String> defaultLabels()
    {
        return Map.of( "cloud", cloud(), "project", project(), "app", app(), "name", name() );
    }

    @Value.Check
    protected void check()
    {
        Preconditions.checkState( app().equals( "xp" ), "field app has to be 'xp' for xp deployments" );
    }
}
