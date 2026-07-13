package com.enonic.kubernetes.client.v1.api.xp7.snapshots;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Xp7MgmtSnapshotsList
{
    private List<Xp7MgmtSnapshotsListSnapshot> results;

    public List<Xp7MgmtSnapshotsListSnapshot> getResults()
    {
        return results;
    }

    public void setResults( final List<Xp7MgmtSnapshotsListSnapshot> results )
    {
        this.results = results;
    }
}
