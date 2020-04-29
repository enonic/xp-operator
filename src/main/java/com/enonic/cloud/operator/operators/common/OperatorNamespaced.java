package com.enonic.cloud.operator.operators.common;

import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.Watcher;

import com.enonic.cloud.operator.common.info.Diff;
import com.enonic.cloud.operator.operators.common.cache.Caches;

public abstract class OperatorNamespaced
    extends Operator
{
    private final static Logger log = LoggerFactory.getLogger( OperatorNamespaced.class );

    @Inject
    public Caches caches;

    protected <R extends HasMetadata, D extends Diff<R>, T extends ResourceInfoNamespaced<R, D>> Optional<T> getInfo( Watcher.Action action,
                                                                                                                      Supplier<T> s )
    {
        try
        {
            return Optional.of( s.get() );
        }
        catch ( Xp7DeploymentNotFound e )
        {
            if ( action != Watcher.Action.DELETED )
            {
                log.warn( e.getMessage() );
            }
            return Optional.empty();
        }
    }

    protected boolean isNamespaceBeingTerminated( ResourceInfoNamespaced info )
    {
        Optional<Namespace> ns = caches.getNamespaceCache().getByName( info.namespace() );
        return ns.isEmpty() || ns.get().getStatus().getPhase().equals( "Terminating" );
    }
}
