package com.enonic.ec.kubernetes.operator.vhosts;

import com.enonic.ec.kubernetes.common.Configuration;

public abstract class XpVHostApply
    extends Configuration
{
    protected String aliasLabelKey()
    {
        return cfgStr( "operator.deployment.xp.labels.pod.name" );
    }
}
