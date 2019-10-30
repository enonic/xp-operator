package com.enonic.ec.kubernetes.deployment.diff;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.deployment.vhost.VHostPath;

@Value.Immutable
public abstract class DiffVHostPath
    extends Diff<VHostPath>
{

    @Value.Derived
    public boolean nodesChanged()
    {
        return !equals( VHostPath::nodes );
    }

}
