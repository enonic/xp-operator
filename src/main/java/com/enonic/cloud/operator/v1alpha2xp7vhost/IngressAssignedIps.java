package com.enonic.cloud.operator.v1alpha2xp7vhost;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.extensions.Ingress;

public class IngressAssignedIps
    implements Function<Ingress, Set<String>>
{
    @Override
    public Set<String> apply( final Ingress ingress )
    {
        Set<String> res = new HashSet<>();

        if ( ingress.getStatus() == null || ingress.getStatus().getLoadBalancer() == null ||
            ingress.getStatus().getLoadBalancer().getIngress() == null )
        {
            return res;
        }

        for ( LoadBalancerIngress i : ingress.getStatus().getLoadBalancer().getIngress() )
        {
            res.add( i.getIp() );
        }

        return res;
    }
}
