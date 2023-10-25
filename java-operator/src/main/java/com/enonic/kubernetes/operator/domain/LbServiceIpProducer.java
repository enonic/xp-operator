package com.enonic.kubernetes.operator.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.Service;

import com.enonic.kubernetes.kubernetes.Clients;

import static com.enonic.kubernetes.common.Configuration.cfgHasKey;
import static com.enonic.kubernetes.common.Configuration.cfgStr;
import static com.enonic.kubernetes.common.SingletonAssert.singletonAssert;

@Singleton
public class LbServiceIpProducer
    implements Supplier<List<String>>
{
    private final Logger log = LoggerFactory.getLogger( LbServiceIpProducer.class );

    @Inject
    Clients clients;

    @ConfigProperty(name = "dns.enabled")
    boolean dnsEnabled;

    public LbServiceIpProducer()
    {
        singletonAssert( this, "constructor" );
    }

    private List<String> getLbIp( final Clients clients )
    {

        if ( cfgHasKey( "dns.lb.cname" ) )
        {
            if ( cfgHasKey( "dns.lb.staticIp" ) )
            {
                throw new RuntimeException( "Both dns.lb.staticIp and dns.lb.cname are set, only one of them can be set" );
            }
            else
            {
                log.info( String.format( "Loadbalancer CNAME is %s", cfgStr( "dns.lb.cname" ) ) );

                return List.of();
            }
        }
        else if ( cfgHasKey( "dns.lb.staticIp" ) )
        {
            final String staticIp = cfgStr( "dns.lb.staticIp" );

            log.info( String.format( "Loadbalancer static IP is %s", staticIp ) );

            return List.of( staticIp );
        }

        final Service lbService =
            clients.k8s().services().inNamespace( cfgStr( "dns.lb.service.namespace" ) ).withName( cfgStr( "dns.lb.service.name" ) ).get();

        if ( lbService == null && dnsEnabled )
        {
            log.warn( "Loadbalancer service not found" );
        }

        final List<String> res = new ArrayList<>();

        if ( lbService != null && lbService.getStatus() != null && lbService.getStatus().getLoadBalancer() != null &&
            lbService.getStatus().getLoadBalancer().getIngress() != null &&
            lbService.getStatus().getLoadBalancer().getIngress().size() == 1 )
        {
            for ( LoadBalancerIngress ingress : lbService.getStatus().getLoadBalancer().getIngress() )
            {
                res.add( ingress.getIp() );
            }
        }

        if ( !res.isEmpty() )
        {
            log.info( String.format( "Loadbalancer IPs are %s", res ) );
        }
        else if ( dnsEnabled )
        {
            log.warn( "Loadbalancer ip not found, domain DNS records wont work" );
        }

        return res;
    }

    @Override
    public List<String> get()
    {
        return getLbIp( clients );
    }
}
