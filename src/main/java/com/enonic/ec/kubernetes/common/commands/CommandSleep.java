package com.enonic.ec.kubernetes.common.commands;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public abstract class CommandSleep
    implements Command<Void>
{
    private static Logger log = LoggerFactory.getLogger( CommandSleep.class );

    protected abstract Long milliSeconds();

    @Override
    public Void execute()
    {
        try
        {
            Thread.sleep( milliSeconds() );
        }
        catch ( InterruptedException e )
        {
            log.warn( "Sleep interrupted" );
        }
        return null;
    }

    @Override
    public String toString()
    {
        return String.format( "SLEEP for %sms", milliSeconds() );
    }
}
