package com.enonic.kubernetes.client.v1;

import com.enonic.kubernetes.client.v1.api.operator.v1.OperatorApi;
import com.enonic.kubernetes.client.v1.api.operator.v1.OperatorApiImpl;
import com.enonic.kubernetes.client.v1.api.xp7.mgmt.Xp7MgmtApi;
import com.enonic.kubernetes.client.v1.api.xp7.mgmt.Xp7MgmtApiImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.http.HttpClient;

public class ClientsImpl implements Clients {

    private final KubernetesClient k8sClient;

    private final HttpClient httpClient;

    private final Config config;

    public ClientsImpl(KubernetesClient k8sClient, HttpClient httpClient, Config config) {
        this.k8sClient = k8sClient;
        this.httpClient = httpClient;
        this.config = config;
    }

    @Override
    public CrdApi crds() {
        return new CrdApiImpl(k8sClient);
    }

    @Override
    public OperatorApi operator() {
        return new OperatorApiImpl(httpClient, config);
    }

    @Override
    public Xp7MgmtApi xp7() {
        return new Xp7MgmtApiImpl(httpClient, config);
    }
}
