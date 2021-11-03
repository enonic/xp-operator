package com.enonic.kubernetes.client;

import com.enonic.kubernetes.client.v1.ClientsImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.HttpClientAware;
import io.fabric8.kubernetes.client.KubernetesClient;
import okhttp3.OkHttpClient;

public class DefaultEnonicKubernetesClient implements EnonicKubernetesClient {

    private final KubernetesClient k8sClient;

    private final OkHttpClient httpClient;

    private final Config config;

    public DefaultEnonicKubernetesClient() {
        this(new DefaultKubernetesClient());
    }

    public DefaultEnonicKubernetesClient(final Config config) {
        this(new DefaultKubernetesClient(config));
    }

    public DefaultEnonicKubernetesClient(final KubernetesClient client) {
        this.config = client.getConfiguration();
        assert client instanceof HttpClientAware;
        this.httpClient = ((HttpClientAware) client).getHttpClient();
        this.k8sClient = client;
    }

    public KubernetesClient k8s() {
        return k8sClient;
    }

    public CustomClient enonic() {
        return () -> new ClientsImpl(k8sClient, httpClient, config);
    }
}
