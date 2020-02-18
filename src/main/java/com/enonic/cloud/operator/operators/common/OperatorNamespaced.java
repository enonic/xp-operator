package com.enonic.cloud.operator.operators.common;

import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watcher;

import com.enonic.cloud.operator.common.info.Diff;

public abstract class OperatorNamespaced
    extends Operator
{
    private final static Logger log = LoggerFactory.getLogger( OperatorNamespaced.class );

    protected <R extends HasMetadata, D extends Diff<R>> Optional<ResourceInfoNamespaced<R, D>> getInfo( Watcher.Action action,
                                                                                                         Supplier<ResourceInfoNamespaced<R, D>> s )
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
}
