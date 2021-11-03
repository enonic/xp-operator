package com.enonic.kubernetes.client.v1;

import com.enonic.kubernetes.client.v1.api.operator.v1.OperatorApi;
import com.enonic.kubernetes.client.v1.api.xp7.mgmt.Xp7MgmtApi;

public interface Clients {
    CrdApi crds();

    OperatorApi operator();

    Xp7MgmtApi xp7();
}
