package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.info;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.operator.common.info.Diff;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;

@Value.Immutable
public abstract class DiffXp7Deployment
    extends Diff<V1alpha2Xp7Deployment>
{
    @Value.Derived
    public DiffXp7DeploymentSpec diffSpec()
    {
        return ImmutableDiffXp7DeploymentSpec.builder().
            oldValue( oldValue().map( V1alpha2Xp7Deployment::getSpec ) ).
            newValue( newValue().map( V1alpha2Xp7Deployment::getSpec ) ).
            build();
    }

    @Value.Check
    protected void check()
    {
        if ( !shouldModify() )
        {
            return;
        }

        Preconditions.checkState( equals( V1alpha2Xp7Deployment::ecCloud ),
                                  "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.cloud" ) + "' cannot be changed" );
        Preconditions.checkState( equals( V1alpha2Xp7Deployment::ecProject ),
                                  "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.project" ) + "' cannot be changed" );
        Preconditions.checkState( equals( V1alpha2Xp7Deployment::ecName ),
                                  "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.name" ) + "' cannot be changed" );
    }
}
