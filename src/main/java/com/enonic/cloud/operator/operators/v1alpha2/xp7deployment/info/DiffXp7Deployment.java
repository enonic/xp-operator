package com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.info;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.cloud.operator.common.info.Diff;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;

import static com.enonic.cloud.operator.common.Configuration.cfgIfBool;
import static com.enonic.cloud.operator.common.Configuration.cfgStr;

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
        if ( newValueCreated() || oldValueRemoved() )
        {
            return;
        }

        cfgIfBool( "operator.deployment.xp.labels.strictValidation", () -> {
            Preconditions.checkState( equals( V1alpha2Xp7Deployment::ecCloud ),
                                      "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.cloud" ) + "' cannot be changed" );
            Preconditions.checkState( equals( V1alpha2Xp7Deployment::ecSolution ),
                                      "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.solution" ) +
                                          "' cannot be changed" );
            Preconditions.checkState( equals( V1alpha2Xp7Deployment::ecEnvironment ),
                                      "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.environment" ) +
                                          "' cannot be changed" );
            Preconditions.checkState( equals( V1alpha2Xp7Deployment::ecService ),
                                      "Field 'metadata.labels." + cfgStr( "operator.deployment.xp.labels.service" ) +
                                          "' cannot be changed" );
        } );
    }
}
