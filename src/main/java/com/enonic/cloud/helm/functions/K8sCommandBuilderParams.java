package com.enonic.cloud.helm.functions;

import java.util.List;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.kubernetes.commands.K8sCommandMapper;

@Value.Immutable
@Params
public interface K8sCommandBuilderParams
{
    K8sCommandMapper k8sCommandMapper();

    List<HasMetadata> oldResources();

    List<HasMetadata> newResources();
}
