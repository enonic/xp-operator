package com.enonic.cloud.operator.v1alpha2xp7vhost;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;


import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressRule;

import static com.enonic.cloud.common.Configuration.cfgStr;

public class IngressEnabledHosts
    implements Function<Ingress, Set<String>>
{
    @Override
    public Set<String> apply( final Ingress ingress )
    {
        Set<String> res = new HashSet<>();

        Map<String, String> annotations = ingress.getMetadata().getAnnotations();
        if ( annotations == null )
        {
            return res;
        }

        String enable = annotations.get( cfgStr( "dns.annotations.enable" ) );
        if ( enable == null )
        {
            return res;
        }

        for ( String host : enable.split( "," ) )
        {
            for ( IngressRule rule : ingress.getSpec().getRules() )
            {
                if ( host.equals( rule.getHost() ) )
                {
                    res.add( host );
                }
            }
        }
        return res;
    }
}
