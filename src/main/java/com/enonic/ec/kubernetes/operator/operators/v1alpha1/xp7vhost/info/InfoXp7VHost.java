package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.info;

import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.spec.Xp7VHostSpecMapping;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.ResourceInfoNamespaced;

@Value.Immutable
public abstract class InfoXp7VHost
    extends ResourceInfoNamespaced<Xp7VHostResource, DiffXp7VHost>
{
    @Override
    protected DiffXp7VHost createDiff( final Optional<Xp7VHostResource> oldResource, final Optional<Xp7VHostResource> newResource )
    {
        return ImmutableDiffXp7VHost.builder().
            oldValue( oldResource ).
            newValue( newResource ).
            build();
    }

    @Value.Check
    protected void check()
    {
        newResource().ifPresent( vHost -> checkNode( true, vHost.getSpec().
            mappings().
            stream().
            map( Xp7VHostSpecMapping::node ).
            collect( Collectors.toList() ) ) );
    }
}
