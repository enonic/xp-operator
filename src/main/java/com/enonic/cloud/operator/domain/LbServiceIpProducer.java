package com.enonic.cloud.operator.domain;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.Service;

import com.enonic.cloud.kubernetes.Clients;

import static com.enonic.cloud.common.Configuration.cfgHasKey;
import static com.enonic.cloud.common.Configuration.cfgStr;

@Singleton
public class LbServiceIpProducer
    implements Supplier<List<String>>
{
    private final Logger log = LoggerFactory.getLogger( LbServiceIpProducer.class );

    private List<String> ips;

    protected LbServiceIpProducer()
    {
        // For testing
    }

    @Inject
    public LbServiceIpProducer( final Clients clients )
    {
        ips = getLbIp( clients );
    }

    private List<String> getLbIp( final Clients clients )
    {
        List<String> res = new LinkedList<>();
        if ( cfgHasKey( "dns.lb.staticIp" ) )
        {
            res.add( cfgStr( "dns.lb.staticIp" ) );
        }

        if ( res.isEmpty() )
        {
            Service lbService = clients.k8s().services().
                inNamespace( cfgStr( "dns.lb.service.namespace" ) ).
                withName( cfgStr( "dns.lb.service.name" ) ).
                get();

            if ( lbService == null )
            {
                log.warn( "Loadbalancer service not found" );
            }

            if ( lbService != null && lbService.getStatus() != null && lbService.getStatus().getLoadBalancer() != null &&
                lbService.getStatus().getLoadBalancer().getIngress() != null &&
                lbService.getStatus().getLoadBalancer().getIngress().size() == 1 )
            {
                for ( LoadBalancerIngress ingress : lbService.getStatus().getLoadBalancer().getIngress() )
                {
                    res.add( ingress.getIp() );
                }
            }
        }

        if ( res.isEmpty() )
        {
            log.warn( "Loadbalancer ip not found, domain DNS records wont work" );
        }
        else
        {
            log.info( String.format( "Loadbalancer IPs are %s", res ) );
        }

        return res;
    }

    @Override
    public List<String> get()
    {
        return ips;
    }
}
