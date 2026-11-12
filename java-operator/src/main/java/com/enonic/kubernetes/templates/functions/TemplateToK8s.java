package com.enonic.kubernetes.templates.functions;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;


import io.fabric8.kubernetes.api.model.HasMetadata;

import com.enonic.kubernetes.templates.values.ValueBuilder;
import com.enonic.kubernetes.kubernetes.commands.K8sCommandMapper;

public class TemplateToK8s<T>
    implements Function<TemplateToK8sParams<T>, K8sCommandBuilderParams>
{
    private final K8sCommandMapper k8sCommandMapper;

    private final ValueBuilder<T> valueBuilder;

    private final Templator templator;

    public TemplateToK8s( final K8sCommandMapper k8sCommandMapper, final ValueBuilder<T> valueBuilder, final Templator templator )
    {
        this.k8sCommandMapper = k8sCommandMapper;
        this.valueBuilder = valueBuilder;
        this.templator = templator;
    }

    @Override
    public K8sCommandBuilderParams apply( final TemplateToK8sParams<T> params )
    {
        List<HasMetadata> oldResources = new LinkedList<>();
        List<HasMetadata> newResources = new LinkedList<>();

        params.oldResource().ifPresent( r -> oldResources.addAll( valueBuilder.
            andThen( templator ).
            apply( r ) ) );

        params.newResource().ifPresent( r -> newResources.addAll( valueBuilder.
            andThen( templator ).
            apply( r ) ) );

        return new K8sCommandBuilderParams( k8sCommandMapper, oldResources, newResources );
    }
}
