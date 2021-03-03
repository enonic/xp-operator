package com.enonic.kubernetes.helm.functions;

import java.util.List;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.helm.values.Values;

public interface Templator
    extends Function<Values, List<HasMetadata>>
{
}
