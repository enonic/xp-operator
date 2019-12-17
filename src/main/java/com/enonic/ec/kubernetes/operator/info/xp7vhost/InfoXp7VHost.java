package com.enonic.ec.kubernetes.operator.info.xp7vhost;

import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7vhost.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.crd.xp7vhost.spec.Xp7VHostSpecMapping;
import com.enonic.ec.kubernetes.operator.info.ResourceInfoNamespaced;

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
