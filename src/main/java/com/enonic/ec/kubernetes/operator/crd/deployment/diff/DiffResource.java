package com.enonic.ec.kubernetes.operator.crd.deployment.diff;

import java.util.function.Function;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.operator.crd.deployment.XpDeploymentResource;

@Value.Immutable
public abstract class DiffResource
    extends Diff<XpDeploymentResource>
{
    @Value.Derived
    public ImmutableDiffSpec diffSpec()
    {
        return ImmutableDiffSpec.builder().
            oldValue( oldValue().map( XpDeploymentResource::getSpec ) ).
            newValue( newValue().map( XpDeploymentResource::getSpec ) ).
            build();
    }

    @Value.Check
    protected void check()
    {
        Function<XpDeploymentResource, String> getCloud = labelFunc( cfgStr( "operator.deployment.xp.labels.ec.cloud" ) );
        Function<XpDeploymentResource, String> getProject = labelFunc( cfgStr( "operator.deployment.xp.labels.ec.project" ) );
        Function<XpDeploymentResource, String> getName = labelFunc( cfgStr( "operator.deployment.xp.labels.ec.name" ) );

        if ( shouldAddOrModify() )
        {
            String cloud = getCloud.apply( newValue().get() );
            String project = getProject.apply( newValue().get() );
            String name = getName.apply( newValue().get() );

            String fullName = String.join( "-", cloud, project, name );
            Preconditions.checkState( fullName.equals( newValue().get().getMetadata().getName() ),
                                      "Xp7Deployment name must be equal to <Cloud>-<Project>-<Name> according to labels, i.e: '" +
                                          fullName + "'" );
        }

        if ( !shouldModify() )
        {
            return;
        }

        Preconditions.checkState( equals( getCloud ), "Cannot change deployment label 'cloud'" );
        Preconditions.checkState( equals( getProject ), "Cannot change deployment label 'project'" );
        Preconditions.checkState( equals( getName ), "Cannot change deployment label 'name'" );
    }

    private Function<XpDeploymentResource, String> labelFunc( String key )
    {
        String regex = "^\\w+$";
        return r -> {
            Preconditions.checkState( r.getMetadata() != null && r.getMetadata().getLabels() != null,
                                      "Xp7Deployment labels cannot be empty" );
            String val = r.getMetadata().getLabels().get( key );
            Preconditions.checkState( val != null, "Xp7Deployment label '" + key + "' cannot be null" );
            Preconditions.checkState( val.matches( regex ), "Xp7Deployment label '" + key + "' must match regex '" + regex + "'" );
            return val;
        };
    }
}
