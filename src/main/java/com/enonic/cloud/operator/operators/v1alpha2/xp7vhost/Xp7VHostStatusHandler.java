package com.enonic.cloud.operator.operators.v1alpha2.xp7vhost;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.cloud.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.cloud.operator.crd.CrdStatusState;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.ImmutableV1alpha2Xp7VHostStatus;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.ImmutableV1alpha2Xp7VHostStatusFields;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostStatus;
import com.enonic.cloud.operator.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostStatusFields;
import com.enonic.cloud.operator.kubectl.ImmutableKubeCmd;
import com.enonic.cloud.operator.operators.common.cache.Caches;
import com.enonic.cloud.operator.operators.common.clients.Clients;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.doh.DohAnswer;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.doh.DohResponse;
import com.enonic.cloud.operator.operators.v1alpha2.xp7vhost.doh.DohService;

@Value.Immutable
public abstract class Xp7VHostStatusHandler
    extends TimerTask
{
    private static final Logger log = LoggerFactory.getLogger( Xp7VHostStatusHandler.class );

    protected abstract Caches caches();

    protected abstract Clients clients();

    protected abstract DohService dns();

    protected abstract String actionId();

    @Override
    public void run()
    {
        caches().getVHostCache().getCollection().forEach( this::handleVHost );
    }

    private void handleVHost( final V1alpha2Xp7VHost vHost )
    {
        V1alpha2Xp7VHostStatus oldStatus = vHost.getStatus();

        // If status field is missing, create it
        if ( oldStatus == null )
        {
            oldStatus = ImmutableV1alpha2Xp7VHostStatus.
                builder().
                fields( ImmutableV1alpha2Xp7VHostStatusFields.builder().build() ).
                build();
        }

        // If vHost already is ready just exit
        if ( oldStatus.state().equals( CrdStatusState.READY ) )
        {
            return;
        }

        ImmutableV1alpha2Xp7VHostStatusFields.Builder fieldsBuilder =
            ImmutableV1alpha2Xp7VHostStatusFields.builder().from( oldStatus.fields() );

        // Handle assigned ips
        Map.Entry<CrdStatusState, String> state = handleAssignedIps( vHost, fieldsBuilder );

        // If IPs are ready, check DNS
        if ( state.getKey().equals( CrdStatusState.READY ) )
        {
            state = handleDns( vHost, fieldsBuilder );
        }

        V1alpha2Xp7VHostStatusFields fields = fieldsBuilder.build();

        V1alpha2Xp7VHostStatus newStatus = ImmutableV1alpha2Xp7VHostStatus.builder().
            from( oldStatus ).
            state( state.getKey() ).
            message( state.getValue() ).
            fields( fieldsBuilder.build() ).
            build();

        if ( !Objects.equals( oldStatus, newStatus ) )
        {
            vHost.setStatus( newStatus );
            updateVHostStatus( vHost );
        }
    }


    private Map.Entry<CrdStatusState, String> handleAssignedIps( final V1alpha2Xp7VHost vHost,
                                                                 final ImmutableV1alpha2Xp7VHostStatusFields.Builder builder )
    {
        if ( !vHost.getSpec().options().ingress() )
        {
            return new AbstractMap.SimpleEntry<>( CrdStatusState.READY, null );
        }

        Optional<Ingress> optionalIngress = caches().getIngressCache().
            getByNamespaceAndName( vHost.getMetadata().getNamespace(), vHost.getMetadata().getName() );

        if ( optionalIngress.isEmpty() )
        {
            builder.ipsAssigned( Collections.emptyList() );
            return new AbstractMap.SimpleEntry<>( CrdStatusState.ERROR, "Ingress not created" );
        }

        Ingress ingress = optionalIngress.get();
        if ( ingress.getStatus() == null || ingress.getStatus().getLoadBalancer() == null ||
            ingress.getStatus().getLoadBalancer().getIngress() == null )
        {
            builder.ipsAssigned( Collections.emptyList() );
            return new AbstractMap.SimpleEntry<>( CrdStatusState.PENDING, "Waiting for assigned IPs" );
        }

        List<String> ips =
            ingress.getStatus().getLoadBalancer().getIngress().stream().map( LoadBalancerIngress::getIp ).collect( Collectors.toList() );
        builder.ipsAssigned( ips );

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

    private void updateVHostStatus( final V1alpha2Xp7VHost vHost )
    {
        ImmutableCombinedCommand.Builder commandBuilder = ImmutableCombinedCommand.builder().id( actionId() );
        ImmutableKubeCmd.builder().
            clients( clients() ).
            namespace( vHost.getMetadata().getNamespace() ).
            resource( vHost ).
            build().
            apply( commandBuilder );

        try
        {
            commandBuilder.build().execute();
        }
        catch ( Exception e )
        {
            log.error( String.format( "Failed updating vHost status '%s'", vHost.getMetadata().getName() ), e );
        }
    }
}
