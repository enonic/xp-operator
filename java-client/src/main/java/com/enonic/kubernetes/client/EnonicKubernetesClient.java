package com.enonic.kubernetes.client;

import com.enonic.kubernetes.client.apis.xp7.mgmt.Xp7MgmtApi;
import com.enonic.kubernetes.client.apis.operator.OperatorApi;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import okhttp3.OkHttpClient;

public class EnonicKubernetesClient extends DefaultKubernetesClient {
    public EnonicKubernetesClient() {
    }

    public EnonicKubernetesClient(String masterUrl) {
        super(masterUrl);
    }

    public EnonicKubernetesClient(Config config) {
        super(config);
    }

    public EnonicKubernetesClient(OkHttpClient httpClient, Config config) {
        super(httpClient, config);
    }

    public OperatorApi operator() {
        return new OperatorApi(this.getHttpClient(), this.getConfiguration());
    }

    public Xp7MgmtApi xp7() {
        return new Xp7MgmtApi(this.getHttpClient(), this.getConfiguration());
    }
}
