package com.enonic.ec.kubernetes.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.quarkus.runtime.StartupEvent;

import com.enonic.ec.kubernetes.common.crd.XpDeploymentCache;
import com.enonic.ec.kubernetes.operator.commands.CommandCreateNamespace;
import com.enonic.ec.kubernetes.operator.commands.CommandDeployXP;

@ApplicationScoped
public class Operator
{
    private final Logger log = LoggerFactory.getLogger( Operator.class );

    @Inject
    KubernetesClient defaultClient;

    @Inject
    XpDeploymentCache xpDeploymentCache;

    void onStartup( @Observes StartupEvent _ev )
    {
        log.debug( "Starting operator" );
        xpDeploymentCache.addWatcher( ( action, id, resource ) -> {
            if ( action == Watcher.Action.ADDED )
            {
                try
                {
                    CommandDeployXP.newBuilder().
                        client( defaultClient ).
                        resource( resource ).
                        build().
                        execute();
                }
                catch ( CommandCreateNamespace.NamespaceExists e )
                {
                    log.debug( "This deployment already exists" );
                }
            }
            else if ( action == Watcher.Action.DELETED )
            {

            }
        } );
    }
}
