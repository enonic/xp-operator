package com.enonic.ec.kubernetes.operator.operators.v1alpha1;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;
import org.wildfly.common.annotation.Nullable;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.ec.kubernetes.operator.XpDeploymentNotFound;
import com.enonic.ec.kubernetes.operator.info.Diff;
import com.enonic.ec.kubernetes.operator.info.ResourceInfo;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.crd.client.Xp7DeploymentCache;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.ImmutableInfoXp7Deployment;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7deployment.info.InfoXp7Deployment;

public abstract class ResourceInfoNamespaced<T extends HasMetadata, D extends Diff<T>>
    extends ResourceInfo<T, D>
{
    @Nullable
    public abstract Xp7DeploymentCache xpDeploymentCache();

    protected void checkNode( boolean allowAll, List<String> nodes )
    {
        String allNodes = cfgStr( "operator.deployment.xp.allNodes" );
        for ( final String node : nodes )
        {
            if ( allNodes.equals( node ) )
            {
                Preconditions.checkState( allowAll, "All nodes selector not allowed" );
            }
            else
            {
                Preconditions.checkState( xpDeploymentResource().getSpec().nodes().containsKey( node ),
                                          "XpDeployment '" + xpDeploymentResource().getMetadata().getName() + "' does not contain node '" +
                                              node + "'" );
            }
        }
    }

    @Value.Default
    public Xp7DeploymentResource xpDeploymentResource()
    {
        Preconditions.checkState( xpDeploymentCache() != null, "XpDeploymentCache is null" );
        Optional<Xp7DeploymentResource> res = xpDeploymentCache().getByName( getXpDeploymentName() );
        if ( res.isEmpty() )
        {
            throw new XpDeploymentNotFound( getXpDeploymentName() );
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
