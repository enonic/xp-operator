package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.operator.common.info.Diff;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource;

@Value.Immutable
public abstract class DiffXp7Deployment
    extends Diff<Xp7DeploymentResource>
{
    @Value.Derived
    public DiffXp7DeploymentSpec diffSpec()
    {
        return ImmutableDiffXp7DeploymentSpec.builder().
            oldValue( oldValue().map( Xp7DeploymentResource::getSpec ) ).
            newValue( newValue().map( Xp7DeploymentResource::getSpec ) ).
            build();
    }

    @Value.Check
    protected void check()
    {
        if ( !shouldModify() )
        {
            return;
        }

        Preconditions.checkState( equals( Xp7DeploymentResource::ecCloud ),
                                  "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.cloud" ) + "' cannot be changed" );
        Preconditions.checkState( equals( Xp7DeploymentResource::ecProject ),
                                  "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.project" ) +
                                      "' cannot be changed" );
        Preconditions.checkState( equals( Xp7DeploymentResource::ecName ),
                                  "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.name" ) + "' cannot be changed" );
    }
}
