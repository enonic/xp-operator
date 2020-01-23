package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.info;

import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostSpecMapping;
import com.enonic.ec.kubernetes.operator.operators.ResourceInfoNamespaced;

@Value.Immutable
public abstract class InfoXp7VHost
    extends ResourceInfoNamespaced<V1alpha2Xp7VHost, DiffXp7VHost>
{
    @Override
    protected DiffXp7VHost createDiff( final Optional<V1alpha2Xp7VHost> oldResource, final Optional<V1alpha2Xp7VHost> newResource )
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
            map( V1alpha2Xp7VHostSpecMapping::nodeGroup ).
            collect( Collectors.toList() ) ) );
    }
}
