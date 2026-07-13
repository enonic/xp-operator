package com.enonic.kubernetes.client.v1.api.xp7.snapshots;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Xp7MgmtSnapshotsListSnapshot
{
    private String name;

    private String reason;

    private String state;

    private String timestamp;

    private List<String> indices;

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason( final String reason )
    {
        this.reason = reason;
    }

    public String getState()
    {
        return state;
    }

    public void setState( final String state )
    {
        this.state = state;
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp( final String timestamp )
    {
        this.timestamp = timestamp;
    }

    public List<String> getIndices()
    {
        return indices;
    }

    public void setIndices( final List<String> indices )
    {
        this.indices = indices;
    }
}
