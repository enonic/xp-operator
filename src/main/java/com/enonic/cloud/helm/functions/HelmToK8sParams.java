package com.enonic.cloud.helm.functions;

import java.util.Optional;

import org.immutables.value.Value;

import com.enonic.cloud.common.annotations.Params;

@Value.Immutable
@Params
public interface HelmToK8sParams<T>
{
    Optional<T> oldResource();

    Optional<T> newResource();
}
