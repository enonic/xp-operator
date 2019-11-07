package com.enonic.ec.kubernetes.crd.deployment.diff;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.Diff;
import com.enonic.ec.kubernetes.crd.deployment.vhost.VHostPath;

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
