package com.enonic.ec.kubernetes.operator.crd.deployment.diff;

import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.deployment.spec.SpecNode;
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

    @Value.Derived
    public String deploymentName()
    {
        return resource().getMetadata().getName();
    }

    @Value.Derived
    public String namespaceName()
    {
        return deploymentName();
    }

    @Value.Derived
    public Map<String, String> defaultLabels()
    {
        return resource().getMetadata().getLabels();
    }

    @Value.Derived
    public String sharedStorageName()
    {
        return deploymentName();
    }

    @Value.Derived
    public String allNodesServiceName()
    {
        return deploymentName();
    }

    @Value.Derived
    public String suPassSecretName()
    {
        return "su-pass";
    }

    public Integer defaultMinimumAvailable( SpecNode node )
    {
        if ( !resource().getSpec().isClustered() )
        {
            return 0;
        }
        return ( node.replicas() / 2 ) + 1;
    }

    @Value.Derived
    public Integer minimumMasterNodes()
    {
        return defaultMinimumAvailable( resource().getSpec().nodes().values().stream().filter( SpecNode::isMasterNode ).findAny().get() );
    }

    @Value.Derived
    public Integer minimumDataNodes()
    {
        return defaultMinimumAvailable( resource().getSpec().nodes().values().stream().filter( SpecNode::isDataNode ).findAny().get() );
    }
}
