package com.enonic.ec.kubernetes.operator.operators.v1alpha2.xp7vhost.info;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.info.Diff;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostSpec;
import com.enonic.ec.kubernetes.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostSpecMapping;

@Value.Immutable
public abstract class DiffXp7VHostSpec
    extends Diff<V1alpha2Xp7VHostSpec>
{
    @Value.Derived
    public boolean certificateChanged()
    {
        return !equals( V1alpha2Xp7VHostSpec::certificate );
    }

    @Value.Derived
    public boolean skipIngressChanged()
    {
        return !equals( V1alpha2Xp7VHostSpec::skipIngress );
    }

    @Value.Derived
    public List<DiffXp7VHostSpecMapping> mappingsChanged()
    {
        Map<String, V1alpha2Xp7VHostSpecMapping> oldMappings = new HashMap<>();
        Map<String, V1alpha2Xp7VHostSpecMapping> newMappings = new HashMap<>();

        oldValue().ifPresent( s -> s.mappings().forEach( p -> oldMappings.put( p.source(), p ) ) );
        newValue().ifPresent( s -> s.mappings().forEach( p -> newMappings.put( p.source(), p ) ) );

        return mergeMaps( oldMappings, newMappings, ( o, n ) -> ImmutableDiffXp7VHostSpecMapping.builder().
            oldValue( o ).
            newValue( n ).
            build() );
    }
}
