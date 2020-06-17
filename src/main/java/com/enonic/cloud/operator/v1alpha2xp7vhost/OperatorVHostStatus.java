package com.enonic.cloud.operator.v1alpha2xp7vhost;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.cloud.apis.doh.DohQueryParams;
import com.enonic.cloud.apis.doh.DohQueryParamsImpl;
import com.enonic.cloud.apis.doh.service.DohAnswer;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMapping;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostStatusFields;
import com.enonic.cloud.operator.dns.functions.info.IngressAssignedIps;
import com.enonic.cloud.operator.dns.functions.info.IngressEnabledHosts;
import com.enonic.cloud.operator.helpers.StatusHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;


public class OperatorVHostStatus
    extends StatusHandler<Xp7VHost, Xp7VHostStatus>
{
    private final IngressAssignedIps ingressAssignedIps = new IngressAssignedIps();

    private final IngressEnabledHosts ingressEnabledHosts = new IngressEnabledHosts();

    @Inject
    Clients clients;

    @Inject
    InformerSearcher<Xp7VHost> xp7VHostInformerSearcher;

    @Inject
    InformerSearcher<Ingress> ingressInformerSearcher;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    Function<DohQueryParams, List<DohAnswer>> doh;

    @Override
    protected InformerSearcher<Xp7VHost> informerSearcher()
    {
        return xp7VHostInformerSearcher;
    }

    @Override
    protected Xp7VHostStatus getStatus( final Xp7VHost resource )
    {
        return resource.getXp7VHostStatus();
    }

    @Override
    protected Doneable<Xp7VHost> updateStatus( final Xp7VHost resource, final Xp7VHostStatus newStatus )
    {
        return clients.xp7VHosts().crdClient().
            inNamespace( resource.getMetadata().getNamespace() ).
            withName( resource.getMetadata().getName() ).
            edit().
            withStatus( newStatus );
    }

    @Override
    protected Xp7VHostStatus pollStatus( final Xp7VHost resource )
    {
        // Get old status or create new one
        Xp7VHostStatus currentStatus = resource.getXp7VHostStatus() != null ? resource.getXp7VHostStatus() : new Xp7VHostStatus().
            withState( Xp7VHostStatus.State.PENDING ).
            withMessage( "Created" ).
            withXp7VHostStatusFields( new Xp7VHostStatusFields().
                withPublicIps( new LinkedList<>() ).
                withDnsRecordCreated( false ) );

        // Check for expected ingresses
        long expectedIngresses = countIngresses( resource );
        if ( expectedIngresses == 0 )
        {
            return currentStatus.
                withMessage( "OK" ).
                withState( Xp7VHostStatus.State.READY );
        }

        // List created ingresses
        List<Ingress> createdIngresses = ingressInformerSearcher.
            get( resource.getMetadata().getNamespace() ).
            filter( ingress -> ingress.getMetadata().getAnnotations() != null ).
            filter( ingress -> resource.getMetadata().getName().equals(
                ingress.getMetadata().getAnnotations().get( cfgStr( "operator.helm.charts.Values.annotationKeys.vHostName" ) ) ) ).
            collect( Collectors.toList() );

        if ( expectedIngresses != createdIngresses.size() )
        {
            return currentStatus.
                withMessage( "Waiting for Ingress creation" ).
                withState( Xp7VHostStatus.State.PENDING );
        }

        // Check for public IPs
        if ( currentStatus.getXp7VHostStatusFields().getPublicIps().isEmpty() )
        {
            Set<String> assignedIps = createdIngresses.stream().
                map( ingressAssignedIps ).
                flatMap( Collection::stream ).collect( Collectors.toSet() );
            if ( assignedIps.size() == 0 )
            {
                return currentStatus.
                    withMessage( "Waiting for IP address" ).
                    withState( Xp7VHostStatus.State.PENDING );
            }
            else
            {
                currentStatus.getXp7VHostStatusFields().withPublicIps( new LinkedList<>( assignedIps ) );
            }
        }

        // Check for DNS
        boolean dnsRecordExpected = resource.getXp7VHostSpec().getXp7VHostSpecOptions() == null ||
            resource.getXp7VHostSpec().getXp7VHostSpecOptions().getDnsRecord();

        if ( dnsRecordExpected && !currentStatus.getXp7VHostStatusFields().getPublicIps().isEmpty() &&
            !currentStatus.getXp7VHostStatusFields().getDnsRecordCreated() )
        {
            currentStatus = dnsStatus( createdIngresses.get( 0 ), currentStatus );
        }
        else
        {
            currentStatus = currentStatus.
                withState( Xp7VHostStatus.State.READY ).
                withMessage( "OK" );
        }

        return currentStatus;
    }

    private int countIngresses( final Xp7VHost resource )
    {
        int count = 0;
        for ( Xp7VHostSpecMapping m : resource.getXp7VHostSpec().getXp7VHostSpecMappings() )
        {
            if ( m.getXp7VHostSpecMappingOptions() == null || m.getXp7VHostSpecMappingOptions().getIngress() )
            {
                count++;
            }
        }
        return count;
    }

    private Xp7VHostStatus dnsStatus( final Ingress ingress, final Xp7VHostStatus currentStatus )
    {
        Set<String> hosts = ingressEnabledHosts.apply( ingress );
        Set<String> found = new HashSet<>();
        for ( String host : hosts )
        {
            if ( !doh.apply( DohQueryParamsImpl.of( "A", host ) ).isEmpty() )
            {
                found.add( host );
            }
        }
        if ( hosts.size() != found.size() )
        {
            return currentStatus.
                withState( Xp7VHostStatus.State.PENDING ).
                withMessage( "Waiting for DNS records" );
        }
        else
        {
            return currentStatus.
                withXp7VHostStatusFields( currentStatus.getXp7VHostStatusFields().withDnsRecordCreated( true ) ).
                withMessage( "OK" ).
                withState( Xp7VHostStatus.State.READY );
        }
    }
}
