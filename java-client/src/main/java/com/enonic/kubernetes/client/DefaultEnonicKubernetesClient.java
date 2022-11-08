package com.enonic.kubernetes.client;

import com.enonic.kubernetes.client.v1.ClientsImpl;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.http.HttpClient;

public class DefaultEnonicKubernetesClient
    implements EnonicKubernetesClient
{

    private final KubernetesClient k8sClient;

    private final HttpClient httpClient;

    private final Config config;

    public DefaultEnonicKubernetesClient()
    {
        this( new KubernetesClientBuilder().build() );
    }

    public DefaultEnonicKubernetesClient( final Config config )
    {
        this( new KubernetesClientBuilder().withConfig( config ).build() );
    }

    public DefaultEnonicKubernetesClient( final KubernetesClient client )
    {
        this.config = client.getConfiguration();
        this.httpClient = client.getHttpClient();
        this.k8sClient = client;
    }

    public KubernetesClient k8s()
    {
        return k8sClient;
    }

    public CustomClient enonic()
    {
        return () -> new ClientsImpl( k8sClient, httpClient, config );
    }
}
