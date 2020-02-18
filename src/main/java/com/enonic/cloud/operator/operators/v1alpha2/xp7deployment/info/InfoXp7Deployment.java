package com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.info;

import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.cloud.operator.common.info.ResourceInfo;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;

import static com.enonic.cloud.operator.common.Validator.dns1035;
import static com.enonic.cloud.operator.common.Validator.dns1123;

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

//    @Value.Derived
//    public Map<String, String> defaultLabels()
//    {
//        return resource().getMetadata().getLabels();
//    }

    @Value.Check
    protected void check()
    {
        newResource().ifPresent( resource -> resource.getSpec().nodeGroups().keySet().forEach( k -> dns1123( "nodeId", k ) ) );

        cfgIfBool( "operator.deployment.xp.labels.strictValidation", () -> {
            Preconditions.checkState( resource().ecCloud() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.cloud" ) + "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.cloud" ), resource().ecCloud() );

            Preconditions.checkState( resource().ecProject() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.project" ) + "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.project" ), resource().ecProject() );

            Preconditions.checkState( resource().ecName() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.name" ) + "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.name" ), resource().ecName() );

            String fullName = String.join( "-", resource().ecCloud(), resource().ecProject(), resource().ecName() );
            Preconditions.checkState( deploymentName().equals( fullName ),
                                      "Xp7Deployment name must be equal to <Cloud>-<Project>-<Name> according to labels, i.e: '" +
                                          fullName + "'" );
        } );
    }
}