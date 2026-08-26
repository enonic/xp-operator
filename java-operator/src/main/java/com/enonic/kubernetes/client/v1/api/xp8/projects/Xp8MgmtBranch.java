package com.enonic.kubernetes.client.v1.api.xp8.projects;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Xp8MgmtBranch
{
    private String name;

    private List<Xp8MgmtSite> sites;

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public List<Xp8MgmtSite> getSites()
    {
        return sites;
    }

    public void setSites( final List<Xp8MgmtSite> sites )
    {
        this.sites = sites;
    }
}
