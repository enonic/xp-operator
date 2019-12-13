package com.enonic.ec.kubernetes.operator.info;

import java.util.Optional;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.client.XpDeploymentCache;
import com.enonic.ec.kubernetes.operator.crd.deployment.diff.ImmutableInfoDeployment;
import com.enonic.ec.kubernetes.operator.crd.deployment.diff.InfoDeployment;

public abstract class ResourceInfoNamespaced<T extends HasMetadata, D extends Diff<T>>
    extends ResourceInfo<T, D>
{
    @Nullable
    public abstract XpDeploymentCache xpDeploymentCache();

    @Value.Default
    public XpDeploymentResource xpDeploymentResource()
    {
        Preconditions.checkState( xpDeploymentCache() != null, "XpDeploymentCache is null" );
        Optional<XpDeploymentResource> res = xpDeploymentCache().getByName( getXpDeploymentName() );
        if ( res.isEmpty() )
        {
            throw new XpDeploymentNotFound( getXpDeploymentName() );
        }
        return res.get();
    }

    @Value.Derived
    public InfoDeployment deploymentInfo()
    {
        return ImmutableInfoDeployment.builder().
            oldResource( xpDeploymentResource() ).
            newResource( xpDeploymentResource() ).
            build();
    }

    private String getXpDeploymentName()
    {
        return resource().getMetadata().getNamespace();
    }
}
