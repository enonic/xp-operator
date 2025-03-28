package com.enonic.kubernetes.operator.xp7app;

import com.enonic.kubernetes.client.v1.xp7deployment.Xp7Deployment;
import com.enonic.kubernetes.kubernetes.Informers;
import com.enonic.kubernetes.operator.helpers.InformerEventHandler;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;

/**
 * This operator class updates Xp7App status fields
 */
@ApplicationScoped
public class OperatorXp7AppStatusOnDeployments
    extends InformerEventHandler<Xp7Deployment>
{
    private static final Logger log = LoggerFactory.getLogger( OperatorXp7AppStatusOnDeployments.class );

    @Inject
    HandlerStatus handlerStatus;

    @Inject
    Informers informers;

    void onStart( @Observes StartupEvent ev )
    {
        listen( informers.xp7DeploymentInformer() );
    }

    @Override
    protected void onNewAdd( Xp7Deployment newResource )
    {
        // Do nothing
    }

    @Override
    public void onUpdate( Xp7Deployment oldDeployment, Xp7Deployment newDeployment )
    {
        log.debug("onUpdate App status on Xp7Deployment update: {} in {}", newDeployment.getMetadata().getNamespace(), newDeployment.getMetadata().getName());
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
