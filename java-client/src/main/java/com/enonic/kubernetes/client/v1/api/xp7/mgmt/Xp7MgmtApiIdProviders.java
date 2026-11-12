package com.enonic.kubernetes.client.v1.api.xp7.mgmt;

import com.enonic.kubernetes.client.v1.api.xp7.idproviders.Xp7MgmtIdProvider;

import java.util.List;

public interface Xp7MgmtApiIdProviders
{
    List<Xp7MgmtIdProvider> list();
}
