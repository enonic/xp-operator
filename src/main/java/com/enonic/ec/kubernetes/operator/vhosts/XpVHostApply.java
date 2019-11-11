package com.enonic.ec.kubernetes.operator.vhosts;

import com.enonic.ec.kubernetes.common.Configuration;

public abstract class XpVHostApply
    extends Configuration
{
    protected String aliasLabelKey()
    {
        return cfgStrFmt( "operator.deployment.xp.pod.label.aliasPrefix", "main" ); // TODO: FIX
    }
}
