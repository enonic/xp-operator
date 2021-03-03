package com.enonic.cloud.common.functions;

import java.util.List;
import java.util.function.Function;

import javax.inject.Singleton;

@Singleton
public class RunnableListExecutor
    implements Function<List<? extends Runnable>, Void>
{
    @Override
    public Void apply( final List<? extends Runnable> runnableList )
    {
        runnableList.forEach( Runnable::run );
        return null;
    }
}
