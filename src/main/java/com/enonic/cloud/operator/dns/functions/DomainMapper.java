package com.enonic.cloud.operator.dns.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.immutables.value.Value;

import com.google.common.base.Preconditions;

import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.operator.dns.model.Domain;
import com.enonic.cloud.operator.dns.model.Record;
import com.enonic.cloud.operator.dns.model.RecordImpl;


@Value.Immutable
@Params
public abstract class DomainMapper
    implements Function<Set<Ingress>, Map<Domain, Record>>
{
    protected abstract DomainFromIngress domainFromIngress();

    @Override
    public Map<Domain, Record> apply( final Set<Ingress> ingresses )
    {
        Map<Domain, Record> res = new HashMap<>();

        for ( Ingress ingress : ingresses )
        {
            merge( res, domainFromIngress().apply( ingress ) );
        }

        return res;
    }

    private void merge( final Map<Domain, Record> res, final Map<Domain, Record> tmp )
    {
        for ( Map.Entry<Domain, Record> e : tmp.entrySet() )
        {
            res.put( e.getKey(), merge( e.getValue(), res.get( e.getKey() ) ) );
        }
    }

    private Record merge( final Record a, final Record b )
    {
        Preconditions.checkState( a != null );
        if ( b == null )
        {
            return a;
        }

        // TODO: Log warnings
        return RecordImpl.builder().
            from( a ).
            addAllIps( b.ips() ).
            build();
    }
}
