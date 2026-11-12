package com.enonic.kubernetes.client;

import io.fabric8.kubernetes.client.KubernetesClient;

public interface EnonicKubernetesClient {
    KubernetesClient k8s();

    CustomClient enonic();
}
