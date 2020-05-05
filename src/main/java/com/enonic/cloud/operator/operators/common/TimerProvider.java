package com.enonic.cloud.operator.operators.common;

import java.util.Timer;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

public class TimerProvider
{
    @Produces
    @Singleton
    @Named("tasks")
    public Timer produceTaskTimer()
    {
        return new Timer();
    }
}
