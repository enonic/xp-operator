package com.enonic.ec.kubernetes.operator.operators.dns.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;

import com.enonic.ec.kubernetes.operator.common.Configuration;

@Value.Immutable
public abstract class DnsIngress
    extends Configuration
{
    protected abstract Ingress ingress();

    protected abstract List<Domain> allowedDomains();

    @Value.Derived
    public boolean shouldModify()
    {
        return dnsEnabled() && allowedDomains().size() > 0 && ips().size() > 0;
    }

    @Value.Derived
    public List<Domain> domains()
    {
        List<Domain> res = new LinkedList<>();
        for ( Domain allowedDomain : allowedDomains() )
        {
            for ( IngressRule rule : ingress().getSpec().getRules() )
            {
                if ( annotatedDomains().contains( rule.getHost() ) )
                {
                    if ( rule.getHost().endsWith( allowedDomain.domain() ) )
                    {
                        res.add( ImmutableDomain.builder().domain( rule.getHost() ).zoneId( allowedDomain.zoneId() ).build() );
                    }
                }
            }
        }
        return res;
    }

    @Value.Derived
    protected List<String> annotatedDomains()
    {
        Optional<String> annotation = getAnnotation( cfgStr( "dns.annotations.enable" ) );
        return annotation.map( s -> Arrays.asList( s.split( "," ) ) ).orElse( Collections.emptyList() );
    }

    @Value.Derived
    public boolean dnsEnabled()
    {
        return annotatedDomains().size() > 0;
    }

    @Value.Derived
    public Integer ttl()
    {
        return Integer.parseInt( getAnnotation( cfgStr( "dns.annotations.ttl" ) ).orElse( cfgStr( "dns.default.ttl" ) ) );
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
                    return ingress().getStatus().getLoadBalancer().getIngress().stream().map( LoadBalancerIngress::getIp ).collect(
                        Collectors.toList() );
                }
            }
        }
        return Collections.emptyList();
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
