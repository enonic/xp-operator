package com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.info;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.info.Diff;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.spec.Xp7VHostSpec;
import com.enonic.ec.kubernetes.operator.operators.v1alpha1.xp7vhost.crd.spec.Xp7VHostSpecMapping;

@Value.Immutable
public abstract class DiffXp7VHostSpec
    extends Diff<Xp7VHostSpec>
{
    @Value.Derived
    public boolean certificateChanged()
    {
        return !equals( Xp7VHostSpec::certificate );
    }

    @Value.Derived
    public boolean skipIngressChanged()
    {
        return !equals( Xp7VHostSpec::skipIngress );
    }

    @Value.Derived
    public List<DiffXp7VHostSpecMapping> mappingsChanged()
    {
        Map<String, Xp7VHostSpecMapping> oldMappings = new HashMap<>();
        Map<String, Xp7VHostSpecMapping> newMappings = new HashMap<>();

        oldValue().ifPresent( s -> s.mappings().forEach( p -> oldMappings.put( p.source(), p ) ) );
        newValue().ifPresent( s -> s.mappings().forEach( p -> newMappings.put( p.source(), p ) ) );

        return mergeMaps( oldMappings, newMappings, ( o, n ) -> ImmutableDiffXp7VHostSpecMapping.builder().
            oldValue( o ).
            newValue( n ).
            build() );
    }
}
