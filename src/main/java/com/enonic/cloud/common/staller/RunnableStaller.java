package com.enonic.cloud.common.staller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;


public class RunnableStaller
    extends TimerTask
{
    private final ReentrantLock lock;

    private final Map<String, RunnableStallerParam> changeMap;

    private final Long stallFor;

    public RunnableStaller( final Long stallFor )
    {
        this.lock = new ReentrantLock();
        this.changeMap = new HashMap<>();
        this.stallFor = stallFor;
    }

    public void put( String key, Runnable runnable )
    {
        RunnableStallerParam p = RunnableStallerParamImpl.of( Instant.now(), runnable );

        lock.lock();
        changeMap.put( key, p );
        lock.unlock();
    }

    @Override
    public void run()
    {
        lock.lock();
        for ( Iterator<Map.Entry<String, RunnableStallerParam>> iterator = changeMap.entrySet().iterator(); iterator.hasNext(); )
        {
            Map.Entry<String, RunnableStallerParam> e = iterator.next();
            if ( e.getValue().timestamp().plusMillis( stallFor ).isBefore( Instant.now() ) )
            {
                iterator.remove();
                e.getValue().runnable().run();
            }
        }
        lock.unlock();
    }
}
