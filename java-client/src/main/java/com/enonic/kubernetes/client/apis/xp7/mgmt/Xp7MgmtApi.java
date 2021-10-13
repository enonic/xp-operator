package com.enonic.kubernetes.client.apis.xp7.mgmt;

import com.enonic.kubernetes.client.apis.TargetSelector;

public interface Xp7MgmtApi {
    TargetSelector<Xp7MgmtApiSnapshots> snapshots();

    TargetSelector<Xp7MgmtApiRoutes> routes();

    TargetSelector<Xp7MgmtApiIdProviders> idProviders();
}
