package com.enonic.cloud.common.staller;

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Singleton;


@Singleton
public class TaskRunner
    extends Timer
{
    private final static long defaultDelay = 2000L;

    private final static long defaultPeriod = 2000L;

    public TaskRunner()
    {
        super( true );
    }

    public void schedule( final TimerTask task )
    {
        super.schedule( task, defaultDelay, defaultPeriod );
    }
}
