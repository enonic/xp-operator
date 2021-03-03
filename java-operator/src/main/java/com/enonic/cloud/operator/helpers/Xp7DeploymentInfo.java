package com.enonic.cloud.operator.helpers;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.cloud.kubernetes.client.v1alpha2.xp7deployment.Xp7DeploymentStatus;

import static com.enonic.cloud.kubernetes.Predicates.inNamespace;
import static com.enonic.cloud.kubernetes.Predicates.isDeleted;

@Singleton
public class Xp7DeploymentInfo
{
    @Inject
    Searchers searchers;

    public Optional<Xp7Deployment> get( final String namespace )
    {
        return searchers.xp7Deployment().stream().
            filter( inNamespace( namespace ) ).
            filter( isDeleted().negate() ).
            findFirst();
    }

    public Optional<Xp7Deployment> get( final HasMetadata r )
    {
        return get( r.getMetadata().getNamespace() );
    }

    public boolean xpRunning( final String namespace )
    {
        Optional<Xp7Deployment> deployment = get( namespace );
        if ( deployment.isEmpty() )
        {
            return false;
        }
        return deployment.get().getStatus().getState().equals( Xp7DeploymentStatus.State.RUNNING );
    }

    public boolean xpDeleted( final String namespace )
    {
        return get( namespace ).isEmpty();
    }
}
