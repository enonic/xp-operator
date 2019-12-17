package com.enonic.ec.kubernetes.operator.info.xp7deployment;

import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.operator.crd.xp7deployment.Xp7DeploymentResource;
import com.enonic.ec.kubernetes.operator.crd.xp7deployment.spec.Xp7DeploymentSpecNode;
import com.enonic.ec.kubernetes.operator.info.ResourceInfo;

import static com.enonic.ec.kubernetes.common.Validator.dnsName;

@Value.Immutable
public abstract class InfoXp7Deployment
    extends ResourceInfo<Xp7DeploymentResource, DiffXp7Deployment>
{
    @Override
    protected DiffXp7Deployment createDiff( final Optional<Xp7DeploymentResource> oldResource, final Optional<Xp7DeploymentResource> newResource )
    {
        return ImmutableDiffXp7Deployment.builder().
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

    public Integer defaultMinimumAvailable( Xp7DeploymentSpecNode node )
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
        return defaultMinimumAvailable( resource().getSpec().nodes().values().stream().filter( Xp7DeploymentSpecNode::isMasterNode ).findAny().get() );
    }

    @Value.Derived
    public Integer minimumDataNodes()
    {
        return defaultMinimumAvailable( resource().getSpec().nodes().values().stream().filter( Xp7DeploymentSpecNode::isDataNode ).findAny().get() );
    }

    @Value.Check
    protected void check()
    {
        newResource().ifPresent( resource -> resource.getSpec().nodes().keySet().forEach( k -> dnsName( "nodeId", k ) ) );

        cfgIfBool( "operator.deployment.xp.labels.ec.strictValidation", () -> {
            Preconditions.checkState( resource().ecCloud() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.cloud" ) +
                                          "' is missing" );
            dnsName( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.cloud" ), resource().ecCloud() );

            Preconditions.checkState( resource().ecProject() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.project" ) +
                                          "' is missing" );
            dnsName( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.project" ), resource().ecProject() );

            Preconditions.checkState( resource().ecName() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.name" ) + "' is missing" );
            dnsName( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.name" ), resource().ecName() );

            String fullName = String.join( "-", resource().ecCloud(), resource().ecProject(), resource().ecName() );
            Preconditions.checkState( deploymentName().equals( fullName ),
                                      "Xp7Deployment name must be equal to <Cloud>-<Project>-<Name> according to labels, i.e: '" +
                                          fullName + "'" );
        } );
    }
}
