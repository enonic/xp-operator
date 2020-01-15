package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost;

import java.util.HashMap;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.info.ValueBuilder;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.Xp7VHostResource;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.info.DiffXp7VHost;

@Value.Immutable
public abstract class Xp7VHostValues
    extends ValueBuilder<ResourceInfoNamespaced<Xp7VHostResource, DiffXp7VHost>>
{
    protected abstract String issuer();

    @Override
    protected void createValues( final Map<String, Object> values )
    {
        Map<String, Object> vhost = new HashMap<>();
        vhost.put( "issuer", issuer() );
        vhost.put( "spec", info().resource().getSpec() );
        values.put( "vhost", vhost );
    }
}
