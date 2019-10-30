package com.enonic.ec.kubernetes.deployment.xpdeployment;

import java.util.Optional;

import com.google.common.base.Preconditions;

import com.enonic.ec.kubernetes.deployment.xpdeployment.spec.Spec;
import com.enonic.ec.kubernetes.deployment.xpdeployment.spec.SpecNode;

public class XpDeploymentSpecChangeValidation
{
    public static void checkSpec( Spec oldSpec, Spec newSpec )
    {
        Preconditions.checkState( oldSpec.cloud().equals( newSpec.cloud() ), "cannot change 'cloud'" );
        Preconditions.checkState( oldSpec.project().equals( newSpec.project() ), "cannot change 'project'" );
        Preconditions.checkState( oldSpec.app().equals( newSpec.app() ), "cannot change 'app'" );
        Preconditions.checkState( oldSpec.name().equals( newSpec.name() ), "cannot change 'name'" );
        Preconditions.checkState( oldSpec.sharedDisks().equals( newSpec.sharedDisks() ), "cannot change 'sharedDisks'" );

        for ( SpecNode newNode : newSpec.nodes() )
        {
            Optional<SpecNode> oldNode =
                oldSpec.nodes().stream().filter( n -> n.alias().equals( newNode.alias() ) ).findAny();
            checkNode( oldNode, newNode );
        }
    }

    public static void checkNode( Optional<SpecNode> oldNode, SpecNode newNode )
    {
        if ( oldNode.isEmpty() )
        {
            return;
        }
        Preconditions.checkState( oldNode.get().type().equals( newNode.type() ), "cannot change node 'type'" );
        Preconditions.checkState( oldNode.get().resources().disks().equals( newNode.resources().disks() ), "cannot change node 'disks'" );
    }
}
