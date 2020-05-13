package com.enonic.cloud.operator.v1alpha2xp7vhost;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimerTask;
import java.util.function.Function;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.apis.doh.DohQueryParams;
import com.enonic.cloud.apis.doh.DohQueryParamsImpl;
import com.enonic.cloud.apis.doh.service.DohAnswer;
import com.enonic.cloud.common.staller.TaskRunner;
import com.enonic.cloud.kubernetes.caches.IngressCache;
import com.enonic.cloud.kubernetes.caches.V1alpha2Xp7VHostCache;
import com.enonic.cloud.kubernetes.commands.K8sLogHelper;
import com.enonic.cloud.kubernetes.crd.client.CrdClient;
import com.enonic.cloud.kubernetes.crd.status.CrdStatusState;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.ImmutableV1alpha2Xp7VHostStatus;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.ImmutableV1alpha2Xp7VHostStatusFields;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHost;
import com.enonic.cloud.kubernetes.crd.xp7.v1alpha2.vhost.V1alpha2Xp7VHostStatus;
import com.enonic.cloud.operator.dns.functions.info.IngressAssignedIps;
import com.enonic.cloud.operator.dns.functions.info.IngressEnabledHosts;

import static com.enonic.cloud.common.Configuration.cfgIfBool;


public class OperatorVHostStatus
    extends TimerTask
{
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    CrdClient crdClient;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    V1alpha2Xp7VHostCache v1alpha2Xp7VHostCache;

    @Inject
    IngressCache ingressCache;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    Function<DohQueryParams, List<DohAnswer>> doh;

    @Inject
    TaskRunner taskRunner;

    private final IngressAssignedIps ingressAssignedIps = new IngressAssignedIps();

    private final IngressEnabledHosts ingressEnabledHosts = new IngressEnabledHosts();

    void onStartup( @Observes StartupEvent _ev )
    {
        cfgIfBool("operator.status.enabled",  () -> taskRunner.schedule( this ) );
    }

    @Override
    public void run()
    {
        v1alpha2Xp7VHostCache.
            getStream().
            filter( vHost -> vHost.getMetadata().getDeletionTimestamp() == null ).
            forEach( vhost -> updateStatus( vhost, pollStatus( vhost ) ) );
    }

    private V1alpha2Xp7VHostStatus pollStatus( final V1alpha2Xp7VHost vHost )
    {
        V1alpha2Xp7VHostStatus currentStatus = vHost.getStatus() != null ? vHost.getStatus() : ImmutableV1alpha2Xp7VHostStatus.builder().
            fields( ImmutableV1alpha2Xp7VHostStatusFields.builder().build() ).
            build();

        if ( !vHost.getSpec().options().ingress() )
        {
            return getBuilder( currentStatus ).
                message( "OK" ).
                state( CrdStatusState.READY ).
                build();
        }

        Optional<Ingress> ingress = ingressCache.get( vHost.getMetadata().getNamespace(), vHost.getMetadata().getName() );

        if ( ingress.isEmpty() )
        {
            return getBuilder( currentStatus ).
                state( CrdStatusState.PENDING ).
                message( "Waiting for Ingress creation" ).
                build();
        }

        if ( currentStatus.fields().publicIps().isEmpty() )
        {
            Set<String> assignedIps = ingressAssignedIps.apply( ingress.get() );
            if ( assignedIps.size() == 0 )
            {
                return getBuilder( currentStatus ).
                    state( CrdStatusState.PENDING ).
                    message( "Waiting for IP address" ).
                    build();
            }
            else
            {
                currentStatus = getBuilder( currentStatus ).
                    fields( ImmutableV1alpha2Xp7VHostStatusFields.builder().
                        from( currentStatus.fields() ).
                        addAllPublicIps( assignedIps ).
                        build() ).
                    build();
            }
        }

        if ( vHost.getSpec().options().dnsRecord() && !currentStatus.fields().publicIps().isEmpty() &&
            !currentStatus.fields().dnsRecordCreated() )
        {
            currentStatus = dnsStatus( ingress.get(), currentStatus );
        }
        else
        {
            currentStatus = getBuilder( currentStatus ).
                state( CrdStatusState.READY ).
                message( "OK" ).
                build();
        }

        return currentStatus;
    }

    private V1alpha2Xp7VHostStatus dnsStatus( final Ingress ingress, final V1alpha2Xp7VHostStatus currentStatus )
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
            return getBuilder( currentStatus ).
                from( currentStatus ).
                state( CrdStatusState.PENDING ).
                message( "Waiting for DNS records" ).
                build();
        }
        else
        {
            return getBuilder( currentStatus ).
                from( currentStatus ).
                fields( ImmutableV1alpha2Xp7VHostStatusFields.builder().
                    from( currentStatus.fields() ).
                    dnsRecordCreated( true ).
                    build() ).
                state( CrdStatusState.READY ).
                message( "OK" ).
                build();
        }
    }

    private void updateStatus( final V1alpha2Xp7VHost vHost, final V1alpha2Xp7VHostStatus status )
    {
        if ( status.equals( vHost.getStatus() ) )
        {
            return;
        }

        K8sLogHelper.logDoneable( crdClient.xp7VHosts().
            inNamespace( vHost.getMetadata().getNamespace() ).
            withName( vHost.getMetadata().getName() ).
            edit().
            withStatus( status ) );
    }

    private ImmutableV1alpha2Xp7VHostStatus.Builder getBuilder( V1alpha2Xp7VHostStatus status )
    {
        return ImmutableV1alpha2Xp7VHostStatus.builder().from( status );
    }
}
