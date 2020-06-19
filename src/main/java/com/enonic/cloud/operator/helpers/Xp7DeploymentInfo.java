package com.enonic.cloud.operator.helpers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7deployment.Xp7DeploymentStatus;

@Singleton
public class Xp7DeploymentInfo
{
    @Inject
    Searchers searchers;

    public boolean xpRunning( final String namespace )
    {
        return searchers.xp7Deployment().query().
            inNamespace( namespace ).
            hasNotBeenDeleted().
            filter( d -> d.getXp7DeploymentStatus() != null ).
            filter( d -> d.getXp7DeploymentStatus().getState().equals( Xp7DeploymentStatus.State.RUNNING ) ).
            get().isPresent();
    }

    public boolean xpDeleted( final String namespace )
    {
        return searchers.xp7Deployment().query().
            inNamespace( namespace ).
            hasNotBeenDeleted().get().
            isEmpty();
    }
}
