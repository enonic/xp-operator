package com.enonic.cloud.operator.operators.common;

import java.util.Optional;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.operator.common.info.Diff;
import com.enonic.cloud.operator.common.info.ResourceInfo;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.info.ImmutableInfoXp7Deployment;
import com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.info.InfoXp7Deployment;


public abstract class ResourceInfoNamespaced<T extends HasMetadata, D extends Diff<T>>
    extends ResourceInfo<T, D>
{
    @Nullable
    public abstract Caches caches();

    @Value.Default
    public V1alpha2Xp7Deployment xpDeploymentResource()
    {
        Preconditions.checkState( caches() != null || caches().getDeploymentCache() != null, "XpDeploymentCache is null" );
        Optional<V1alpha2Xp7Deployment> res = caches().getDeploymentCache().get( null, getXpDeploymentName() );
        if ( res.isEmpty() )
        {
            throw new Xp7DeploymentNotFound( getXpDeploymentName() );
        }
        return res.get();
    }

    @Value.Derived
    public InfoXp7Deployment deploymentInfo()
    {
        return ImmutableInfoXp7Deployment.builder().
            oldResource( xpDeploymentResource() ).
            newResource( xpDeploymentResource() ).
            build();
    }

    private String getXpDeploymentName()
    {
        return resource().getMetadata().getNamespace();
    }
}