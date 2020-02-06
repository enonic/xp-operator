package com.enonic.ec.kubernetes.operator.operators.common.cache;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Caches
{
    private final AppCache appCache;

    private final ConfigCache configCache;

    private final ConfigMapCache configMapCache;

    private final DeploymentCache deploymentCache;

    private final IngressCache ingressCache;

    private final VHostCache vHostCache;

    @Inject
    public Caches( final AppCache appCache, final ConfigCache configCache, final ConfigMapCache configMapCache,
                   final DeploymentCache deploymentCache, final IngressCache ingressCache, final VHostCache vHostCache )
    {
        this.appCache = appCache;
        this.configCache = configCache;
        this.configMapCache = configMapCache;
        this.deploymentCache = deploymentCache;
        this.ingressCache = ingressCache;
        this.vHostCache = vHostCache;
    }

    public AppCache getAppCache()
    {
        return appCache;
    }

    public ConfigCache getConfigCache()
    {
        return configCache;
    }

    public ConfigMapCache getConfigMapCache()
    {
        return configMapCache;
    }

    public DeploymentCache getDeploymentCache()
    {
        return deploymentCache;
    }

    public IngressCache getIngressCache()
    {
        return ingressCache;
    }

    public VHostCache getVHostCache()
    {
        return vHostCache;
    }
}
