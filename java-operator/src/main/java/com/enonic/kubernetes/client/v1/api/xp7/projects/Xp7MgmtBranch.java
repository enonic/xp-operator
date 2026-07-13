package com.enonic.kubernetes.client.v1.api.xp7.projects;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Xp7MgmtBranch
{
    private String name;

    private List<Xp7MgmtSite> sites;

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public List<Xp7MgmtSite> getSites()
    {
        return sites;
    }

    public void setSites( final List<Xp7MgmtSite> sites )
    {
        this.sites = sites;
    }
}
