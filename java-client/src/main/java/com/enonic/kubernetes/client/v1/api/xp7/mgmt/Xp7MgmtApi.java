package com.enonic.kubernetes.client.v1.api.xp7.mgmt;

import com.enonic.kubernetes.client.v1.api.xp7.TargetSelector;

public interface Xp7MgmtApi
{
    TargetSelector<Xp7MgmtApiSnapshots> snapshots();

    TargetSelector<Xp7MgmtApiIdProviders> idProviders();

    TargetSelector<Xp7MgmtApiProjects> projects();

    TargetSelector<Xp7MgmtApiWebapps> webapps();
}
