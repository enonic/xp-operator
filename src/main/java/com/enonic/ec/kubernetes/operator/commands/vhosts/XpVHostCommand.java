package com.enonic.ec.kubernetes.operator.commands.vhosts;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.crd.vhost.XpVHostResource;

public abstract class XpVHostCommand
    extends Configuration
    implements CombinedCommandBuilder
{
    public String getIngressName( XpVHostResource resource )
    {
        return resource.getMetadata().getName();
    }
}
