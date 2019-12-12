package com.enonic.ec.kubernetes.operator.crd.deployment.diff;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.info.ResourceInfo;

@Value.Immutable
public abstract class InfoDeployment
    extends ResourceInfo<XpDeploymentResource, DiffResource>
{
    @Override
    protected DiffResource createDiff( final Optional<XpDeploymentResource> oldResource, final Optional<XpDeploymentResource> newResource )
    {
        return ImmutableDiffResource.builder().
            oldValue( oldResource ).
            newValue( newResource ).
            build();
    }
}
