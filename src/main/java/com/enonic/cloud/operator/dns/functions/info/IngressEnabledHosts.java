package com.enonic.cloud.operator.dns.functions.info;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;

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
