package com.enonic.kubernetes.client.v1.api.operator;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorVersion
{
    private String gitCommit;

    private String gitTags;

    private String buildDate;

    private String version;

    private String gitTreeState;

    public String getGitCommit()
    {
        return gitCommit;
    }

    public void setGitCommit( final String gitCommit )
    {
        this.gitCommit = gitCommit;
    }

    public String getGitTags()
    {
        return gitTags;
    }

    public void setGitTags( final String gitTags )
    {
        this.gitTags = gitTags;
    }

    public String getBuildDate()
    {
        return buildDate;
    }

    public void setBuildDate( final String buildDate )
    {
        this.buildDate = buildDate;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( final String version )
    {
        this.version = version;
    }

    public String getGitTreeState()
    {
        return gitTreeState;
    }

    public void setGitTreeState( final String gitTreeState )
    {
        this.gitTreeState = gitTreeState;
    }
}
