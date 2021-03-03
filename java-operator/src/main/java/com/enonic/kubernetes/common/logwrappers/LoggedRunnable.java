package com.enonic.kubernetes.common.logwrappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.annotation.Nullable;

public abstract class LoggedRunnable
    implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger( LoggedRunnable.class );

    public abstract Runnable wrappedRunnable();

    @Nullable
    protected abstract String beforeMessage();

    @Nullable
    public abstract String afterMessage();

    @Override
    public void run()
    {
        if ( beforeMessage() != null )
        {
            log.info( beforeMessage() );
        }
        wrappedRunnable().run();
        if ( afterMessage() != null )
        {
            log.info( afterMessage() );
        }
    }
}
