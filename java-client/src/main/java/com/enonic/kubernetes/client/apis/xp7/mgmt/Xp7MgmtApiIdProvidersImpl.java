package com.enonic.kubernetes.client.apis.xp7.mgmt;

import com.enonic.kubernetes.client.api.xp7.idproviders.Xp7MgmtIdProvidersList;
import io.fabric8.kubernetes.client.Config;
import okhttp3.OkHttpClient;

public class Xp7MgmtApiIdProvidersImpl extends Xp7MgmtClient implements Xp7MgmtApiIdProviders {
    public Xp7MgmtApiIdProvidersImpl(OkHttpClient client, Config config, String namespace, String name, String nodeGroup) {
        super(client, config, namespace, name, nodeGroup);
    }

    public Xp7MgmtIdProvidersList list() {
        return request(requestBuilder("cloud-utils/idproviders"), Xp7MgmtIdProvidersList.class);
    }
}
