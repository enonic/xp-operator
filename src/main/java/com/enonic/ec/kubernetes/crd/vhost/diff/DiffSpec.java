package com.enonic.ec.kubernetes.crd.vhost.diff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.crd.vhost.spec.Spec;
import com.enonic.ec.kubernetes.crd.vhost.spec.SpecMapping;

@Value.Immutable
public abstract class DiffSpec
    extends Diff<Spec>
{
    @Value.Derived
    public boolean certificateChanged()
    {
        return !equals( Spec::certificate );
    }

    @Value.Derived
    public List<DiffSpecMapping> mappingsChanged()
    {
        Map<String, SpecMapping> oldMappings = new HashMap<>();
        Map<String, SpecMapping> newMappings = new HashMap<>();

        oldValue().ifPresent( s -> s.mappings().forEach( p -> oldMappings.put( p.source(), p ) ) );
        newValue().ifPresent( s -> s.mappings().forEach( p -> newMappings.put( p.source(), p ) ) );

        return mergeMaps( oldMappings, newMappings, ( o, n ) -> ImmutableDiffSpecMapping.builder().
            oldValue( o ).
            newValue( n ).
            build() );
    }
}
