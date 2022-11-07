package com.enonic.kubernetes.client.v1.api.xp7.mgmt;

import com.enonic.kubernetes.client.v1.api.xp7.idproviders.Xp7MgmtIdProvidersList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;

public class Xp7MgmtApiIdProvidersImpl extends Xp7MgmtClient implements Xp7MgmtApiIdProviders {
    public Xp7MgmtApiIdProvidersImpl(HttpClient client, Config config, String namespace, String name, String nodeGroup) {
        super(client, config, namespace, name, nodeGroup);
    }

    public Xp7MgmtIdProvidersList list() {
        return request( requestUrl( "cloud-utils/idproviders"), Xp7MgmtIdProvidersList.class);
    }
}
