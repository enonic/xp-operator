package com.enonic.kubernetes.client.v1.api.xp7.projects;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Xp7MgmtSite
{
    private String displayName;

    private String path;

    private String language;

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName( final String displayName )
    {
        this.displayName = displayName;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( final String path )
    {
        this.path = path;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage( final String language )
    {
        this.language = language;
    }
}
