package com.enonic.kubernetes.kubernetes.commands;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.common.logwrappers.LoggedRunnable;

@NullMarked
public final class K8sCommand
    extends LoggedRunnable
{
    private final K8sCommandAction action;

    private final HasMetadata resource;

    private final Runnable wrappedRunnable;

    public K8sCommand( final K8sCommandAction action, final HasMetadata resource, final Runnable wrappedRunnable )
    {
        this.action = action;
        this.resource = resource;
        this.wrappedRunnable = wrappedRunnable;
    }

    public K8sCommandAction action()
    {
        return action;
    }

    public HasMetadata resource()
    {
        return resource;
    }

    @Override
    public Runnable wrappedRunnable()
    {
        return wrappedRunnable;
    }

    @Override
    protected String beforeMessage()
    {
        return K8sLogHelper.log( action(), resource() );
    }

    @Override
    public @Nullable String afterMessage()
    {
        return null;
    }
}
