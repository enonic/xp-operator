package com.enonic.ec.kubernetes.crd.deployment.diff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.crd.deployment.vhost.VHost;
import com.enonic.ec.kubernetes.crd.deployment.vhost.VHostPath;

@Value.Immutable
public abstract class DiffVHost
    extends Diff<VHost>
{

    @Value.Derived
    public boolean certificateChanged()
    {
        return !equals( VHost::certificate );
    }

    @Value.Derived
    public List<DiffVHostPath> pathsChanged()
    {
        Map<String, VHostPath> oldPaths = new HashMap<>();
        Map<String, VHostPath> newPaths = new HashMap<>();

        oldValue().ifPresent( s -> s.vHostPaths().forEach( p -> oldPaths.put( p.path(), p ) ) );
        newValue().ifPresent( s -> s.vHostPaths().forEach( p -> newPaths.put( p.path(), p ) ) );

        return mergeMaps( oldPaths, newPaths, ( o, n ) -> ImmutableDiffVHostPath.builder().
            oldValue( o ).
            newValue( n ).
            build() );
    }

}
