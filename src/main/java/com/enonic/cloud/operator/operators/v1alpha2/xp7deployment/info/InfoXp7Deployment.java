package com.enonic.cloud.operator.operators.v1alpha2.xp7deployment.info;

import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.deployment.V1alpha2Xp7Deployment;
import com.enonic.cloud.operator.operators.common.ResourceInfoNamespaced;

import static com.enonic.cloud.operator.common.Configuration.cfgIfBool;
import static com.enonic.cloud.operator.common.Configuration.cfgStr;
import static com.enonic.cloud.operator.common.Validator.dns1035;
import static com.enonic.cloud.operator.common.Validator.dns1123;

@Value.Immutable
public abstract class InfoXp7Deployment
    extends ResourceInfoNamespaced<V1alpha2Xp7Deployment, DiffXp7Deployment>
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

//    @Override
//    public V1alpha2Xp7Deployment xpDeploymentResource()
//    {
//        return resource();
//    }
//
//    @Override
//    public InfoXp7Deployment deploymentInfo()
//    {
//        return this;
//    }
//
//    @Value.Derived
//    public String deploymentName()
//    {
//        return resource().getMetadata().getName();
//    }

    @Value.Check
    protected void check()
    {
        newResource().ifPresent( resource -> resource.getSpec().nodeGroups().keySet().forEach( k -> dns1123( "nodeId", k ) ) );

        cfgIfBool( "operator.deployment.xp.labels.strictValidation", () -> {
            Preconditions.checkState( resource().ecCloud() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.cloud" ) + "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.cloud" ), resource().ecCloud() );

            Preconditions.checkState( resource().ecSolution() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.solution" ) +
                                          "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.solution" ), resource().ecSolution() );

            Preconditions.checkState( resource().ecEnvironment() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.environment" ) +
                                          "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.environment" ), resource().ecEnvironment() );

            Preconditions.checkState( resource().ecService() != null,
                                      "Label '" + "metadata.labels." + cfgStr( "operator.deployment.xp.labels.service" ) + "' is missing" );
            dns1035( "metadata.labels." + cfgStr( "operator.deployment.xp.labels.service" ), resource().ecService() );

            String fullName =
                String.join( "-", resource().ecCloud(), resource().ecSolution(), resource().ecEnvironment(), resource().ecService() );
            Preconditions.checkState( name().equals( fullName ),
                                      "Xp7Deployment name must be equal to <Cloud>-<Solution>-<Environment>-<Service> according to labels, i.e: '" +
                                          fullName + "'" );
        } );
    }
}
