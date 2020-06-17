package com.enonic.cloud.operator.helpers;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;

import com.enonic.cloud.kubernetes.InformerStarter;

public abstract class InformerEventHandler<R extends HasMetadata>
    implements ResourceEventHandler<R>
{
    private static final Logger log = LoggerFactory.getLogger( InformerEventHandler.class );

    @Inject
    InformerStarter informerStarter;

    protected void listenToInformer( SharedIndexInformer<R> indexInformer )
    {
        // This lag is introduced so old resources do not count as "added" when the operator starts
        new Thread( () -> {
            while ( !indexInformer.hasSynced() )
            {
                try
                {
                    Thread.sleep( 100L );
                }
                catch ( InterruptedException e )
                {
                    // Ignore
                }
            }
            log.info( String.format( "Adding listener %s", this.getClass().getSimpleName() ) );
            indexInformer.addEventHandler( this );
        } ).start();
    }
}
