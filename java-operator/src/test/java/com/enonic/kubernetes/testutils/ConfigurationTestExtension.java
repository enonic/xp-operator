package com.enonic.kubernetes.testutils;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.micronaut.context.ApplicationContext;

public class ConfigurationTestExtension
    implements BeforeAllCallback
{
    private static final AtomicBoolean STARTED = new AtomicBoolean( false );

    @Override
    public void beforeAll( final ExtensionContext context )
    {
        if ( STARTED.compareAndSet( false, true ) )
        {
            ApplicationContext.builder().deduceEnvironment( false ).start();
        }
    }
}
