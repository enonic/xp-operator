package com.enonic.cloud.operator.dns.functions.info;

import java.util.Map;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.extensions.Ingress;

import static com.enonic.cloud.common.Configuration.cfgStr;

public class IngressTTL
    implements Function<Ingress, Integer>
{
    @Override
    public Integer apply( final Ingress ingress )
    {
        Map<String, String> annotations = ingress.getMetadata().getAnnotations();
        if ( annotations == null )
        {
            return null;
        }

        return Integer.valueOf( annotations.getOrDefault( cfgStr( "dns.annotations.ttl" ), cfgStr( "dns.default.ttl" ) ) );
    }
}
