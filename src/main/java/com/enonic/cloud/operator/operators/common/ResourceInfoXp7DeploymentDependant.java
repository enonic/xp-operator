package com.enonic.cloud.operator.operators.common;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.operator.common.info.Diff;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.operators.common.cache.Caches;


public abstract class ResourceInfoXp7DeploymentDependant<T extends HasMetadata, D extends Diff<T>>
    extends ResourceInfoNamespaced<T, D>
{
    public abstract Caches caches();

    @Nullable
    @Value.Derived
    public V1alpha2Xp7Deployment xpDeploymentResource()
    {
        Preconditions.checkState( caches() != null || caches().getDeploymentCache() != null, "XpDeploymentCache is null" );
        List<V1alpha2Xp7Deployment> res = caches().getDeploymentCache().getByNamespace( namespace() ).collect( Collectors.toList() );
        if ( res.isEmpty() )
        {
            if ( resourceBeingRestoredFromBackup() )
            {
                return null;
            }
            else
            {
                throw new Xp7DeploymentNotFound( namespace() );
            }
        }
        Preconditions.checkState( res.size() == 1, "Multiple Xp7Deployments found in the same namespace" );
        return res.get( 0 );
    }
}
