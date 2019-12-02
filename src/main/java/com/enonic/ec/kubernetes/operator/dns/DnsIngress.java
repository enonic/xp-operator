package com.enonic.ec.kubernetes.operator.dns;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.ec.kubernetes.common.Configuration;

@Value.Immutable
public abstract class DnsIngress
    extends Configuration
{
    protected abstract Ingress ingress();

    protected abstract List<AllowedDomain> allowedDomains();

    @Value.Derived
    public Optional<String> subDomain()
    {
        return getAnnotation( cfgStr( "dns.annotations.enable" ) );
    }

    @Value.Derived
    public String ttl()
    {
        return getAnnotation( cfgStr( "dns.annotations.ttl" ) ).orElse( cfgStr( "dns.default.ttl" ) );
    }

    @Value.Derived
    public List<String> ips()
    {
        if ( ingress().getStatus() != null )
        {
            if ( ingress().getStatus().getLoadBalancer() != null )
            {
                if ( ingress().getStatus().getLoadBalancer().getIngress() != null )
                {
                    return ingress().getStatus().getLoadBalancer().getIngress().stream().map( i -> i.getIp() ).collect(
                        Collectors.toList() );
                }
            }
        }
        return Collections.EMPTY_LIST;
    }

    @Value.Derived
    public Optional<AllowedDomain> allowedDomain()
    {
        if ( subDomain().isPresent() )
        {
            for ( AllowedDomain allowedDomain : allowedDomains() )
            {
                if ( subDomain().get().endsWith( "." + allowedDomain.domain() ) )
                {
                    return Optional.of( allowedDomain );
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> getAnnotation( String key )
    {
        if ( ingress().getMetadata() != null )
        {
            if ( ingress().getMetadata().getAnnotations() != null )
            {
                return Optional.ofNullable( ingress().getMetadata().getAnnotations().get( key ) );
            }
        }
        return Optional.empty();
    }
}
