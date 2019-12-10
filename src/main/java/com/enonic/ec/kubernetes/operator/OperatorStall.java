package com.enonic.ec.kubernetes.operator;

import com.enonic.ec.kubernetes.common.Configuration;

public class OperatorStall
    extends Configuration
{
    protected synchronized void stall( Runnable r )
    {
        waitSome();
        r.run();
    }

    protected void waitSome()
    {
        try
        {
            Thread.sleep( 500 );
        }
        catch ( InterruptedException e )
        {
            // Do nothing
        }
    }
}
