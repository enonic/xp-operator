package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7deployment.info;

import java.util.Map;
import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.operator.common.info.ResourceInfo;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7DeploymentSpecNode;

import static com.enonic.ec.kubernetes.operator.common.Validator.dns1035;
import static com.enonic.ec.kubernetes.operator.common.Validator.dns1123;

@Value.Immutable
public abstract class InfoXp7Deployment
    extends ResourceInfo<V1alpha2Xp7Deployment, DiffXp7Deployment>
{
    @Override
    protected DiffXp7Deployment createDiff( final Optional<V1alpha2Xp7Deployment> oldResource,
                                            final Optional<V1alpha2Xp7Deployment> newResource )
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

    public Integer defaultMinimumAvailable( V1alpha2Xp7DeploymentSpecNode node )
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
        return defaultMinimumAvailable(
            resource().getSpec().nodeGroups().values().stream().filter( V1alpha2Xp7DeploymentSpecNode::master ).findAny().get() );
    }

    @Value.Derived
    public Integer minimumDataNodes()
    {
        return defaultMinimumAvailable(
            resource().getSpec().nodeGroups().values().stream().filter( V1alpha2Xp7DeploymentSpecNode::data ).findAny().get() );
    }

    @Value.Check
    protected void check()
    {
        newResource().ifPresent( resource -> resource.getSpec().nodeGroups().keySet().forEach( k -> dns1123( "nodeId", k ) ) );

        cfgIfBool( "operator.deployment.xp.labels.ec.strictValidation", () -> {
            Preconditions.checkState( resource().ecCloud() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.cloud" ) +
                                          "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.cloud" ), resource().ecCloud() );

            Preconditions.checkState( resource().ecProject() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.project" ) +
                                          "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.project" ), resource().ecProject() );

            Preconditions.checkState( resource().ecName() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.name" ) + "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.ec.name" ), resource().ecName() );

            String fullName = String.join( "-", resource().ecCloud(), resource().ecProject(), resource().ecName() );
            Preconditions.checkState( deploymentName().equals( fullName ),
                                      "Xp7Deployment name must be equal to <Cloud>-<Project>-<Name> according to labels, i.e: '" +
                                          fullName + "'" );
        } );
    }
}
