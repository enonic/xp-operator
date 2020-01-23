package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost;

import java.util.HashMap;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.info.ValueBuilder;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.ec.kubernetes.operator.operators.ResourceInfoNamespaced;
import com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.info.DiffXp7VHost;

@Value.Immutable
public abstract class Xp7VHostValues
    extends ValueBuilder<ResourceInfoNamespaced<V1alpha2Xp7VHost, DiffXp7VHost>>
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
