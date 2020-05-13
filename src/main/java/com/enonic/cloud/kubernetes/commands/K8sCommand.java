package com.enonic.cloud.kubernetes.commands;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.common.logwrappers.LoggedRunnable;

@Value.Immutable
public abstract class K8sCommand
    extends LoggedRunnable
{
    protected abstract K8sCommandAction action();

    protected abstract HasMetadata resource();

    @Override
    protected String beforeMessage()
    {
        return K8sLogHelper.log( action(), resource() );
    }
}
