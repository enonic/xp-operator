package com.enonic.cloud.operator.v1alpha2xp7vhost;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.apis.doh.DohQueryParams;
import com.enonic.cloud.apis.doh.DohQueryParamsImpl;
import com.enonic.cloud.apis.doh.service.DohAnswer;
import com.enonic.cloud.common.staller.TaskRunner;
import com.enonic.cloud.kubernetes.Clients;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHost;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostSpecMapping;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostStatus;
import com.enonic.cloud.kubernetes.model.v1alpha2.xp7vhost.Xp7VHostStatusFields;
import com.enonic.cloud.operator.dns.functions.info.IngressAssignedIps;
import com.enonic.cloud.operator.dns.functions.info.IngressEnabledHosts;

import static com.enonic.cloud.common.Configuration.cfgIfBool;
import static com.enonic.cloud.common.Configuration.cfgLong;
import static com.enonic.cloud.common.Configuration.cfgStr;


public class OperatorVHostStatus
    implements Runnable
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

    @Inject
    TaskRunner taskRunner;

    @ConfigProperty(name = "operator.tasks.statusDelaySeconds")
    Long statusDelay;

    void onStartup( @Observes StartupEvent _ev )
    {
        cfgIfBool( "operator.status.enabled", () -> taskRunner.scheduleAtFixedRate( this, cfgLong( "operator.tasks.initialDelayMs" ),
                                                                                    cfgLong( "operator.tasks.vHost.status.periodMs" ),
                                                                                    TimeUnit.MILLISECONDS ) );
    }

    @Override
    public void run()
    {
        xp7VHostInformerSearcher.
            getStream().
            filter( vHost -> vHost.getMetadata().getDeletionTimestamp() == null ).
            filter( vHost -> Duration.between( Instant.parse( vHost.getMetadata().getCreationTimestamp() ), Instant.now() ).getSeconds() >
                statusDelay ).
            forEach( vhost -> updateStatus( vhost, vhost.getXp7VHostSpec().hashCode(), pollStatus( vhost ) ) );
    }

    private Xp7VHostStatus pollStatus( final Xp7VHost vHost )
    {
        Xp7VHostStatus currentStatus = vHost.getXp7VHostStatus() != null ? vHost.getXp7VHostStatus() : new Xp7VHostStatus().
            withState( Xp7VHostStatus.State.PENDING ).
            withMessage( "Created" ).
            withXp7VHostStatusFields( new Xp7VHostStatusFields().
                withPublicIps( new LinkedList<>() ).
                withDnsRecordCreated( false ) );

        long expectedIngresses = countIngresses( vHost );

        if ( expectedIngresses == 0 )
        {
            return currentStatus.
                withMessage( "OK" ).
                withState( Xp7VHostStatus.State.READY );
        }

        List<Ingress> createdIngresses = ingressInformerSearcher.get( vHost.getMetadata().getNamespace() ).
            filter( ingress -> ingress.getMetadata().getAnnotations() != null ).
            filter( ingress -> Objects.equals( vHost.getMetadata().getName(), ingress.getMetadata().getAnnotations().get(
                cfgStr( "operator.helm.charts.Values.annotationKeys.vHostName" ) ) ) ).
            collect( Collectors.toList() );

        if ( expectedIngresses != createdIngresses.size() )
        {
            return currentStatus.
                withMessage( "Waiting for Ingress creation" ).
                withState( Xp7VHostStatus.State.PENDING );
        }

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
                currentStatus.getXp7VHostStatusFields().withPublicIps( new ArrayList<>( assignedIps ) );
            }
        }

        boolean dnsRecordExpected =
            vHost.getXp7VHostSpec().getXp7VHostSpecOptions() == null || vHost.getXp7VHostSpec().getXp7VHostSpecOptions().getDnsRecord();

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

    private int countIngresses( final Xp7VHost vHost )
    {
        int count = 0;
        for ( Xp7VHostSpecMapping m : vHost.getXp7VHostSpec().getXp7VHostSpecMappings() )
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

    private void updateStatus( final Xp7VHost vHost, final int oldStatusHashCode, final Xp7VHostStatus status )
    {
        if ( status.equals( vHost.getXp7VHostStatus() ) )
        {
            return;
        }

        K8sLogHelper.logDoneable( clients.xp7VHosts().crdClient().
            inNamespace( vHost.getMetadata().getNamespace() ).
            withName( vHost.getMetadata().getName() ).
            edit().
            withStatus( status ) );
    }
}
