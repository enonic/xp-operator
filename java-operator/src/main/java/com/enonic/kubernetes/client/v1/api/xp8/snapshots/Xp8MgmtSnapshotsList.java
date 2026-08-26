package com.enonic.kubernetes.client.v1.api.xp8.snapshots;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Xp8MgmtSnapshotsList
{
    private List<Xp8MgmtSnapshotsListSnapshot> results;

    public List<Xp8MgmtSnapshotsListSnapshot> getResults()
    {
        return results;
    }

    public void setResults( final List<Xp8MgmtSnapshotsListSnapshot> results )
    {
        this.results = results;
    }
}
