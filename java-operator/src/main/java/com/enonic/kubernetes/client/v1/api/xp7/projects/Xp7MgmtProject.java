package com.enonic.kubernetes.client.v1.api.xp7.projects;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Xp7MgmtProject
{
    private String name;

    private String displayName;

    private String description;

    private String parent;

    private List<Xp7MgmtBranch> branches;

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName( final String displayName )
    {
        this.displayName = displayName;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( final String description )
    {
        this.description = description;
    }

    public String getParent()
    {
        return parent;
    }

    public void setParent( final String parent )
    {
        this.parent = parent;
    }

    public List<Xp7MgmtBranch> getBranches()
    {
        return branches;
    }

    public void setBranches( final List<Xp7MgmtBranch> branches )
    {
        this.branches = branches;
    }
}
