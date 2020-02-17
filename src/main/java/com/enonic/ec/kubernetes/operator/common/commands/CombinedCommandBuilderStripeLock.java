package com.enonic.ec.kubernetes.operator.common.commands;


import java.util.concurrent.locks.Lock;

import org.immutables.value.Value;

import com.google.common.util.concurrent.Striped;

public abstract class CombinedCommandBuilderStripeLock
    implements CombinedCommandBuilder
{
    public abstract Striped<Lock> locks();

    @Value.Default
    public Long accumulationTimeMs()
    {
        return 2000L;
    }

    @Override
    public final void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        // Acquire a lock
        Lock lock = locks().get( produceLockKey() );
        lock.lock();

        // Let resources accumulate
        try
        {
            Thread.sleep( accumulationTimeMs() );
        }
        catch ( InterruptedException e )
        {
            // Ignore
        }

        synchronizedAddCommands( commandBuilder );

        // Unlock
        lock.unlock();
    }

    protected abstract String produceLockKey();

    protected abstract void synchronizedAddCommands( final ImmutableCombinedCommand.Builder commandBuilder );
}
