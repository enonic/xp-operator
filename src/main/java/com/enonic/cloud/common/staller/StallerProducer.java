package com.enonic.cloud.common.staller;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

public class StallerProducer
{
    @Singleton
    @Produces
    @Named("1200ms")
    public RunnableStaller staller1200ms( final TaskRunner taskRunner )
    {

        return addStaller( taskRunner, 1200L );
    }

    @Singleton
    @Produces
    @Named("400ms")
    public RunnableStaller staller400ms( final TaskRunner taskRunner )
    {
        return addStaller( taskRunner, 400L );
    }

    private static RunnableStaller addStaller( final TaskRunner taskRunner, final long stallFor )
    {
        RunnableStaller staller = new RunnableStaller( stallFor );
        taskRunner.schedule( staller, stallFor, stallFor );
        return staller;
    }
}
