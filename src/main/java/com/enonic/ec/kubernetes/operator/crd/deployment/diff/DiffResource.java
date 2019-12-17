package com.enonic.ec.kubernetes.operator.crd.deployment.diff;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;

@Value.Immutable
public abstract class DiffResource
    extends Diff<XpDeploymentResource>
{
    @Value.Derived
    public ImmutableDiffSpec diffSpec()
    {
        return ImmutableDiffSpec.builder().
            oldValue( oldValue().map( XpDeploymentResource::getSpec ) ).
            newValue( newValue().map( XpDeploymentResource::getSpec ) ).
            build();
    }

    @Value.Check
    protected void check()
    {
        if ( !shouldModify() )
        {
            return;
        }

        Preconditions.checkState( equals( XpDeploymentResource::ecCloud ),
                                  "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.cloud" ) + "' cannot be changed" );
        Preconditions.checkState( equals( XpDeploymentResource::ecProject ),
                                  "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.project" ) +
                                      "' cannot be changed" );
        Preconditions.checkState( equals( XpDeploymentResource::ecName ),
                                  "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.name" ) + "' cannot be changed" );
    }
}
