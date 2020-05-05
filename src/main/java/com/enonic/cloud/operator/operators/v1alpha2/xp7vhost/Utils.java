package com.enonic.cloud.operator.operators.v1alpha2.xp7vhost;

import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.ImmutableV1alpha2Xp7VHostStatus;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;

public class Utils
{
    public static ImmutableV1alpha2Xp7VHostStatus.Builder createStatusBuilder( V1alpha2Xp7VHost vHost )
    {
        ImmutableV1alpha2Xp7VHostStatus.Builder builder = ImmutableV1alpha2Xp7VHostStatus.builder();
        if ( vHost.getStatus() != null )
        {
            builder.from( vHost.getStatus() );
        }
        return builder;
    }
}
