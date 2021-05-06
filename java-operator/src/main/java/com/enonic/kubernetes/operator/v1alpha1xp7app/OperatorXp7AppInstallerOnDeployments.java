package com.enonic.kubernetes.operator.v1alpha1xp7app;

import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.enonic.kubernetes.operator.v1alpha2xp7deployment.Predicates.running;

/**
 * This operator class installs/uninstalls apps in XP
 */
@Singleton
public class OperatorXp7AppInstallerOnDeployments
    extends InformerEventHandler<Xp7Deployment>
{
    @Inject
    OperatorXp7AppInstaller operatorXp7AppInstaller;

    @Override
    public void onNewAdd( final Xp7Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onUpdate( final Xp7Deployment oldResource, final Xp7Deployment newResource )
    {
        // If deployment just came online, trigger app update
        if (running().test( newResource ) && oldResource.getStatus().getState() != newResource.getStatus().getState()) {
            operatorXp7AppInstaller.run();
        }
    }

    @Override
    public void onDelete( final Xp7Deployment oldResource, final boolean b )
    {
        // Do nothing
    }
}
