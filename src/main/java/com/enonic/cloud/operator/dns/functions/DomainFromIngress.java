package com.enonic.cloud.operator.dns.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.immutables.value.Value;

import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.operator.dns.functions.info.IngressAssignedIps;
import com.enonic.cloud.operator.dns.functions.info.IngressCDN;
import com.enonic.cloud.operator.dns.functions.info.IngressEnabledHosts;
import com.enonic.cloud.operator.dns.functions.info.IngressHeritage;
import com.enonic.cloud.operator.dns.functions.info.IngressTTL;
import com.enonic.cloud.operator.dns.model.Domain;
import com.enonic.cloud.operator.dns.model.DomainImpl;
import com.enonic.cloud.operator.dns.model.Record;
import com.enonic.cloud.operator.dns.model.RecordImpl;


@Value.Immutable
@Params
public abstract class DomainFromIngress
    implements Function<Ingress, Map<Domain, Record>>
{
    protected abstract Set<Domain> allowedDomains();

    @Override
    public Map<Domain, Record> apply( final Ingress ingress )
    {
        Map<Domain, Record> res = new HashMap<>();

        for ( String host : new IngressEnabledHosts().apply( ingress ) )
        {
            Set<String> assignedIps = new IngressAssignedIps().apply( ingress );
            String heritage = new IngressHeritage().apply( ingress );
            Integer ttl = new IngressTTL().apply( ingress );
            Boolean cdn = new IngressCDN().apply( ingress );
            for ( Domain allowed : allowedDomains() )
            {
                if ( host.endsWith( allowed.domain() ) )
                {
                    if ( heritage != null || assignedIps != null || ttl != null )
                    {
                        res.put( DomainImpl.of( host, allowed.zoneId() ), RecordImpl.of( ttl, assignedIps, cdn, heritage ) );
                    }
                }
            }
        }

        return res;
    }
}
