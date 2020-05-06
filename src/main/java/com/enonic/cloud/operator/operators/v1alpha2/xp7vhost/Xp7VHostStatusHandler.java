package com.enonic.cloud.operator.operators.v1alpha2.xp7vhost;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.cloud.operator.crd.CrdStatusState;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.ImmutableV1alpha2Xp7VHostStatus;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.ImmutableV1alpha2Xp7VHostStatusFields;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostStatus;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostStatusFields;
import com.enonic.cloud.operator.operators.common.StatusHandler;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.doh.DohAnswer;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.doh.DohResponse;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.doh.DohService;

@Value.Immutable
public abstract class Xp7VHostStatusHandler
    extends StatusHandler<V1alpha2Xp7VHostStatusFields, V1alpha2Xp7VHostStatus, V1alpha2Xp7VHost>
{
    private static final Logger log = LoggerFactory.getLogger( Xp7VHostStatusHandler.class );

    protected abstract DohService dns();

    @Override
    protected Stream<V1alpha2Xp7VHost> getResourcesToUpdate( final Caches caches )
    {
        return caches.getVHostCache().getCollection().stream();
    }

    @Override
    protected V1alpha2Xp7VHostStatus createDefaultStatus( final V1alpha2Xp7VHost r )
    {
        return ImmutableV1alpha2Xp7VHostStatus.
            builder().
            fields( ImmutableV1alpha2Xp7VHostStatusFields.builder().build() ).
            build();
    }

    @Override
    protected V1alpha2Xp7VHostStatus createNewStatus( final Caches caches, final V1alpha2Xp7VHost vHost,
                                                      final V1alpha2Xp7VHostStatus oldStatus )
    {
        // If vHost already is ready just exit
        if ( oldStatus.state().equals( CrdStatusState.READY ) )
        {
            log.debug( String.format( "vHost '%s' in NS '%s' already in READY state", vHost.getMetadata().getName(),
                                      vHost.getMetadata().getNamespace() ) );
            return oldStatus;
        }

        ImmutableV1alpha2Xp7VHostStatusFields.Builder fieldsBuilder =
            ImmutableV1alpha2Xp7VHostStatusFields.builder().from( oldStatus.fields() );

        // Handle assigned ips
        log.debug( String.format( "Checking provisioned ips for vHost '%s' in NS '%s'", vHost.getMetadata().getName(),
                                  vHost.getMetadata().getNamespace() ) );
        Map.Entry<CrdStatusState, String> state = handleAssignedIps( caches, vHost, fieldsBuilder );

        // If IPs are ready, check DNS
        if ( state.getKey().equals( CrdStatusState.READY ) )
        {
            log.debug( String.format( "Checking DNS records for vHost '%s' in NS '%s'", vHost.getMetadata().getName(),
                                      vHost.getMetadata().getNamespace() ) );
            state = handleDns( vHost, fieldsBuilder );
        }

        return ImmutableV1alpha2Xp7VHostStatus.builder().
            from( oldStatus ).
            state( state.getKey() ).
            message( state.getValue() == null ? "OK" : state.getValue() ).
            fields( fieldsBuilder.build() ).
            build();
    }

    private Map.Entry<CrdStatusState, String> handleAssignedIps( final Caches caches, final V1alpha2Xp7VHost vHost,
                                                                 final ImmutableV1alpha2Xp7VHostStatusFields.Builder builder )
    {
        if ( !vHost.getSpec().options().ingress() )
        {
            return new AbstractMap.SimpleEntry<>( CrdStatusState.READY, null );
        }

        Optional<Ingress> optionalIngress = caches.getIngressCache().
            getByNamespaceAndName( vHost.getMetadata().getNamespace(), vHost.getMetadata().getName() );

        if ( optionalIngress.isEmpty() )
        {
            builder.publicIps( Collections.emptyList() );
            return new AbstractMap.SimpleEntry<>( CrdStatusState.ERROR, "Ingress not created" );
        }

        Ingress ingress = optionalIngress.get();
        if ( ingress.getStatus() == null || ingress.getStatus().getLoadBalancer() == null ||
            ingress.getStatus().getLoadBalancer().getIngress() == null )
        {
            builder.publicIps( Collections.emptyList() );
            return new AbstractMap.SimpleEntry<>( CrdStatusState.PENDING, "Waiting for assigned IPs" );
        }

        List<String> ips =
            ingress.getStatus().getLoadBalancer().getIngress().stream().map( LoadBalancerIngress::getIp ).collect( Collectors.toList() );
        builder.publicIps( ips );

        if ( ips.isEmpty() )
        {
            return new AbstractMap.SimpleEntry<>( CrdStatusState.PENDING, "Waiting for assigned IPs" );
        }
        else
        {
            return new AbstractMap.SimpleEntry<>( CrdStatusState.READY, null );
        }
    }

    private Map.Entry<CrdStatusState, String> handleDns( V1alpha2Xp7VHost vHost,
                                                         final ImmutableV1alpha2Xp7VHostStatusFields.Builder builder )
    {
        if ( !vHost.getSpec().options().dnsRecord() )
        {
            return new AbstractMap.SimpleEntry<>( CrdStatusState.READY, null );
        }

        DohResponse res;
        try
        {
            res = dns().query( "A", vHost.getSpec().host() );
        }
        catch ( Exception e )
        {
            log.error( String.format( "Failed to query dns for '%s'", vHost.getSpec().host() ), e );
            return new AbstractMap.SimpleEntry<>( CrdStatusState.ERROR, "Failed to query DNS" );
        }

        for ( DohAnswer answer : res.answers() )
        {
            if ( answer.name().equals( vHost.getSpec().host() ) )
            {
                builder.dnsRecordCreated( true );
                return new AbstractMap.SimpleEntry<>( CrdStatusState.READY, null );
            }
        }
        return new AbstractMap.SimpleEntry<>( CrdStatusState.PENDING, "DNS not updated" );
    }
}
