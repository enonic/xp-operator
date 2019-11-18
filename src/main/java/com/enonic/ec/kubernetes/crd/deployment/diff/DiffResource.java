package com.enonic.ec.kubernetes.crd.deployment.diff;

import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.crd.deployment.XpDeploymentResource;

@Value.Immutable
public abstract class DiffResource
    extends Diff<XpDeploymentResource>
{
    @Value.Derived
    public ImmutableDiffSpec diffSpec()
    {
        return ImmutableDiffSpec.builder().
            oldValue( Optional.ofNullable( oldValue().map( XpDeploymentResource::getSpec ).orElse( null ) ) ).
            newValue( Optional.ofNullable( newValue().map( XpDeploymentResource::getSpec ).orElse( null ) ) ).
            build();
    }

    @Value.Check
    protected void check()
    {
        if ( shouldAddOrModify() )
        {
            checkNew( newValue().get() );
        }

        if ( !shouldModify() )
        {
            return;
        }

        Preconditions.checkState( equals( XpDeploymentResource::getEcCloud ), "Cannot change deployment cloud" );
        Preconditions.checkState( equals( XpDeploymentResource::getEcProject ), "Cannot change deployment project" );
        Preconditions.checkState( equals( XpDeploymentResource::getEcType ), "Cannot change deployment type" );
        Preconditions.checkState( equals( XpDeploymentResource::getEcName ), "Cannot change deployment name" );
    }

    private void checkNew( XpDeploymentResource resource )
    {
        String regex = "^\\w+$";

        Preconditions.checkState( resource.getMetadata().getLabels() != null, "Deployment is missing labels" );

        String cloud = checkLabel( resource, regex, "ec-cloud" );
        String project = checkLabel( resource, regex, "ec-project" );
        String type = checkLabel( resource, regex, "ec-type" );
        Preconditions.checkState( type.equals( "xp" ), "Deployment label 'ec-type' must be 'xp'" );
        String name = checkLabel( resource, regex, "ec-name" );

        String fullName = String.join( "-", cloud, project, type, name );
        Preconditions.checkState( fullName.equals( resource.getMetadata().getName() ),
                                  "Deployment name must be equal to <Cloud>-<Project>-<Type>-<Name>, i.e: '" + fullName + "'" );
    }

    private String checkLabel( XpDeploymentResource resource, String regex, String key )
    {

        String label = resource.getMetadata().getLabels().get( key );
        Preconditions.checkState( label != null, "Deployment is missing label '" + key + "'" );
        Preconditions.checkState( label.matches( regex ), "Deployment label '" + key + "' must match regex '" + regex + "'" );
        return label;
    }

}
