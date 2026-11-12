package com.enonic.kubernetes.templates.functions;

import org.jspecify.annotations.NullMarked;

import java.util.Optional;



@NullMarked
public record TemplateToK8sParams<T>( Optional<T> oldResource, Optional<T> newResource )
{
}
