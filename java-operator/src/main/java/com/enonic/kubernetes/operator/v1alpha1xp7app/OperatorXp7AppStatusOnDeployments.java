package com.enonic.kubernetes.operator.v1alpha1xp7app;

import com.enonic.kubernetes.client.v1alpha2.Xp7Deployment;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

/**
 * This operator class updates Xp7App status fields
 */
@Singleton
public class OperatorXp7AppStatusOnDeployments
    extends InformerEventHandler<Xp7Deployment>
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7AppStatusOnDeployments.class );

    @Inject
    HandlerStatus handlerStatus;

    @Override
    protected void onNewAdd( Xp7Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onUpdate( Xp7Deployment oldDeployment, Xp7Deployment newDeployment )
    {
        try {
            handlerStatus.updateStatus( newDeployment );
        } catch (IOException e) {
            log.warn( String.format( "Failed updating app status in NS %s: %s", newDeployment.getMetadata().getNamespace(),
                e.getMessage() ) );
        }
    }

    @Override
    public void onDelete( Xp7Deployment oldDeployment, boolean b )
    {
        // Do nothing
    }
}
