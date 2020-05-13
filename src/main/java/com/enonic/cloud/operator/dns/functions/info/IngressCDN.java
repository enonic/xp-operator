package com.enonic.cloud.operator.dns.functions.info;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.extensions.Ingress;

import static com.enonic.cloud.common.Configuration.cfgStr;

public class IngressCDN
    implements Function<Ingress, Boolean>
{
    @Override
    public Boolean apply( final Ingress ingress )
    {
        Map<String, String> annotations = ingress.getMetadata().getAnnotations();
        if ( annotations == null )
        {
            return false;
        }
        return Objects.equals( annotations.get( cfgStr( "dns.annotations.cdn" ) ), "true" );
    }
}
