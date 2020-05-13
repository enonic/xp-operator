package com.enonic.cloud.kubernetes.crd;

import java.util.Arrays;

public abstract class BuilderException
    extends RuntimeException
{
    protected BuilderException( String... missingAttributes )
    {
        super( "Failed to build object" );
        this.initCause( createCause( missingAttributes ) );
    }

    @SuppressWarnings("WeakerAccess")
    protected IllegalStateException createCause( String... missingAttributes )
    {
        return new IllegalStateException( "Some fields in '" + getFieldPath() + "' are missing: " + Arrays.asList( missingAttributes ) );
    }

    protected abstract String getFieldPath();
}