package com.enonic.cloud.helm.functions;

import java.util.List;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.helm.values.Values;

public interface Templator
    extends Function<Values, List<HasMetadata>>
{
}
