package com.enonic.cloud.operator.operators.v1alpha1.xp7app.info;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.operator.crd.xp7.v1alpha1.app.V1alpha1Xp7App;
import com.enonic.cloud.operator.operators.common.ResourceInfoXp7DeploymentDependant;

@Value.Immutable
public abstract class InfoXp7App
    extends ResourceInfoXp7DeploymentDependant<V1alpha1Xp7App, DiffXp7App>
{
    @Override
    protected DiffXp7App createDiff( final Optional<V1alpha1Xp7App> oldResource, final Optional<V1alpha1Xp7App> newResource )
    {
        return ImmutableDiffXp7App.builder().
            oldValue( oldResource ).
            newValue( newResource ).
            build();
    }
}
