package com.enonic.cloud.helm.functions;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.helm.values.ValueBuilder;
import com.enonic.cloud.kubernetes.commands.K8sCommandMapper;

@Value.Immutable
@Params
public abstract class HelmToK8s<T>
    implements Function<HelmToK8sParams<T>, K8sCommandBuilderParams>
{
    protected abstract K8sCommandMapper k8sCommandMapper();

    protected abstract ValueBuilder<T> valueBuilder();

    protected abstract Templator templator();

    @Override
    public K8sCommandBuilderParams apply( final HelmToK8sParams<T> params )
    {
        List<HasMetadata> oldResources = new LinkedList<>();
        List<HasMetadata> newResources = new LinkedList<>();

        params.oldResource().ifPresent( r -> oldResources.addAll( valueBuilder().
            andThen( templator() ).
            apply( r ) ) );

        params.newResource().ifPresent( r -> newResources.addAll( valueBuilder().
            andThen( templator() ).
            apply( r ) ) );

        return K8sCommandBuilderParamsImpl.of( k8sCommandMapper(), oldResources, newResources );
    }
}
