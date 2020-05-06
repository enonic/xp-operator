package com.enonic.cloud.operator.kubectl.base;

import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public abstract class KubeCommandOptions
{
    protected abstract Optional<Boolean> alwaysOverwrite();

    protected abstract Optional<Boolean> alwaysUpdate();

    protected abstract Optional<Boolean> neverOverwrite();
}
