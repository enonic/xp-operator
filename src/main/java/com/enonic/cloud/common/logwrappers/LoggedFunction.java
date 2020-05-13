package com.enonic.cloud.common.logwrappers;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.annotation.Nullable;

public abstract class LoggedFunction<T, R>
    implements Function<T, R>
{
    private static final Logger log = LoggerFactory.getLogger( LoggedFunction.class );

    public abstract Function<T, R> wrappedFunction();

    @Nullable
    protected abstract Function<T, String> beforeMessage();

    @Nullable
    public abstract Function<R, String> afterMessage();

    @Override
    public R apply( final T t )
    {
        if ( beforeMessage() != null )
        {
            log.info( beforeMessage().apply( t ) );
        }
        R r = wrappedFunction().apply( t );
        if ( beforeMessage() != null )
        {
            log.info( afterMessage().apply( r ) );
        }
        return r;
    }
}
