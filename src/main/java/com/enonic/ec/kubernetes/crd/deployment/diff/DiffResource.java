package com.enonic.ec.kubernetes.crd.deployment.diff;

import java.util.Optional;

import org.immutables.value.Value;

import com.google.common.base.Function;
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
        Function<XpDeploymentResource, String> getCloud = labelFunc( cfgStr( "operator.deployment.xp.labels.ec.cloud" ) );
        Function<XpDeploymentResource, String> getProject = labelFunc( cfgStr( "operator.deployment.xp.labels.ec.project" ) );
        Function<XpDeploymentResource, String> getType = labelFunc( cfgStr( "operator.deployment.xp.labels.ec.type" ) );
        Function<XpDeploymentResource, String> getName = labelFunc( cfgStr( "operator.deployment.xp.labels.ec.name" ) );

        if ( shouldAddOrModify() )
        {
            String cloud = getCloud.apply( newValue().get() );
            String project = getProject.apply( newValue().get() );
            String type = getType.apply( newValue().get() );
            String name = getName.apply( newValue().get() );

            String fullName = String.join( "-", cloud, project, type, name );
            Preconditions.checkState( fullName.equals( newValue().get().getMetadata().getName() ),
                                      "Deployment name must be equal to <Cloud>-<Project>-<Type>-<Name>, i.e: '" + fullName + "'" );
        }

        if ( !shouldModify() )
        {
            return;
        }

        Preconditions.checkState( equals( getCloud ), "Cannot change deployment cloud" );
        Preconditions.checkState( equals( getProject ), "Cannot change deployment project" );
        Preconditions.checkState( equals( getType ), "Cannot change deployment type" );
        Preconditions.checkState( equals( getName ), "Cannot change deployment name" );
    }

    private Function<XpDeploymentResource, String> labelFunc( String key )
    {
        String regex = "^\\w+$";
        return r -> {
            Preconditions.checkState( r.getMetadata().getLabels() != null, "XpSolution labels cannot be null" );
            String val = r.getMetadata().getLabels().get( key );
            Preconditions.checkState( val != null, "XpSolution label '" + key + "' cannot be null" );
            Preconditions.checkState( val.matches( regex ), "XpSolution label '" + key + "' must match regex '" + regex + "'" );
            return val;
        };
    }
}
