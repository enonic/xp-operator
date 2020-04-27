package com.enonic.cloud.operator.helm.v1alpha2.xp7vhost;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.operator.operators.common.ResourceInfoXp7DeploymentDependant;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.DiffXp7VHost;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.info.ImmutableDiffXp7VHost;

@SuppressWarnings("unused")
@Value.Immutable
public abstract class TestInfoXp7VHost
    extends ResourceInfoXp7DeploymentDependant<V1alpha2Xp7VHost, DiffXp7VHost>
{
    public abstract V1alpha2Xp7Deployment overrideDeployment();

    @Override
    public V1alpha2Xp7Deployment xpDeploymentResource()
    {
        return overrideDeployment();
    }

    @Override
    protected DiffXp7VHost createDiff( final Optional<V1alpha2Xp7VHost> oldResource, final Optional<V1alpha2Xp7VHost> newResource )
    {
        return ImmutableDiffXp7VHost.builder().
            oldValue( oldResource ).
            newValue( newResource ).
            build();
    }
}
