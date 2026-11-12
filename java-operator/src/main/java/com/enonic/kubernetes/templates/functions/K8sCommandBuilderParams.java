package com.enonic.kubernetes.templates.functions;

import org.jspecify.annotations.NullMarked;

import java.util.List;


import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.kubernetes.commands.K8sCommandMapper;

@NullMarked
public record K8sCommandBuilderParams(
    K8sCommandMapper k8sCommandMapper,
    List<HasMetadata> oldResources,
    List<HasMetadata> newResources )
{
}
