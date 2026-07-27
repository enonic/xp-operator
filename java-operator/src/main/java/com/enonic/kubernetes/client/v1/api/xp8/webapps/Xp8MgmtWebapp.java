package com.enonic.kubernetes.client.v1.api.xp8.webapps;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Xp8MgmtWebapp
{
    private String name;

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }
}
