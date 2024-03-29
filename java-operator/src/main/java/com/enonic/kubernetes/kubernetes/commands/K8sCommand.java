package com.enonic.kubernetes.kubernetes.commands;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.common.logwrappers.LoggedRunnable;

@Value.Immutable
public abstract class K8sCommand
    extends LoggedRunnable
{
    public abstract K8sCommandAction action();

    public abstract HasMetadata resource();

    @Override
    protected String beforeMessage()
    {
        return K8sLogHelper.log( action(), resource() );
    }
}
