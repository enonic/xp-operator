package com.enonic.cloud.operator.dns;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.extensions.Ingress;

import com.enonic.cloud.common.functions.RunnableListExecutor;
import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.operator.dns.functions.DomainFromIngress;
import com.enonic.cloud.operator.dns.functions.DomainFromIngressImpl;
import com.enonic.cloud.operator.dns.functions.DomainMapper;
import com.enonic.cloud.operator.dns.functions.DomainMapperImpl;
import com.enonic.cloud.operator.dns.functions.DomainSyncer;
import com.enonic.cloud.operator.dns.functions.DomainSyncerParams;
import com.enonic.cloud.operator.dns.functions.DomainSyncerParamsImpl;
import com.enonic.cloud.operator.dns.functions.RelevantIngressPredicateImpl;
import com.enonic.cloud.operator.dns.functions.info.IngressEnabledHosts;
import com.enonic.cloud.operator.dns.model.Domain;
import com.enonic.cloud.operator.dns.model.DomainImpl;
import com.enonic.cloud.operator.dns.model.Record;
import com.enonic.cloud.operator.helpers.InformerEventHandler;

import static com.enonic.cloud.common.Configuration.cfgStr;


@Singleton
public class OperatorDns
    extends InformerEventHandler<Ingress>
{
    private final IngressEnabledHosts ingressEnabledHosts = new IngressEnabledHosts();

    @Inject
    Searchers searchers;

    @Inject
    DomainSyncer domainSyncer;

    @Inject
    RunnableListExecutor runnableListExecutor;

    @ConfigProperty(name = "dns.allowedDomains.keys")
    List<String> allowedDomainKeys;

    private DomainFromIngress domainFromIngress;

    private DomainMapper domainMapper;

    @Override
    public void init()
    {
        List<Domain> allowedDomains = allowedDomainKeys.stream().
            map( k -> DomainImpl.of( cfgStr( "dns.domain." + k ), cfgStr( "dns.zoneId." + k ) ) ).
            collect( Collectors.toList() );

        domainFromIngress = DomainFromIngressImpl.of( allowedDomains );
        domainMapper = DomainMapperImpl.of( domainFromIngress );
    }

    @Override
    public void onNewAdd( final Ingress newResource )
    {
        // Ignore because ingresses are never assigned ips instantly
    }

    @Override
    public void onUpdate( final Ingress oldResource, final Ingress newResource )
    {
        if ( !Objects.equals( getIPs( oldResource ), getIPs( newResource ) ) )
        {
            handle( newResource, false );
        }
    }

    @Override
    public void onDelete( final Ingress oldResource, final boolean b )
    {
        if ( !getIPs( oldResource ).isEmpty() )
        {
            handle( oldResource, true );
        }
    }

    private void handle( final Ingress ingress, boolean deleted )
    {
        Set<Ingress> ingresses = searchers.ingress().query().
            filter( RelevantIngressPredicateImpl.of( ingress, ingressEnabledHosts ) ).
            set();

        domainMapper.
            andThen( createDomainSyncerParamsFunc( ingress, deleted ) ).
            andThen( domainSyncer ).
            andThen( runnableListExecutor ).
            apply( ingresses );
    }

    private Function<Map<Domain, Record>, DomainSyncerParams> createDomainSyncerParamsFunc( final Ingress ingress, boolean deleted )
    {
        return ( Map<Domain, Record> records ) -> {
            DomainSyncerParamsImpl.Builder builder = DomainSyncerParamsImpl.builder();
            builder.currentRecords( records );
            if ( deleted )
            {
                builder.markedForDeletion( domainFromIngress.apply( ingress ) );
            }
            return builder.build();
        };
    }

    public List<String> getIPs( Ingress ingress )
    {
        List<String> res = new LinkedList<>();
        if ( ingress.getStatus() == null )
        {
            return res;
        }
        if ( ingress.getStatus().getLoadBalancer() == null )
        {
            return res;
        }
        if ( ingress.getStatus().getLoadBalancer().getIngress() == null )
        {
            return res;
        }
        for ( LoadBalancerIngress i : ingress.getStatus().getLoadBalancer().getIngress() )
        {
            if ( i.getIp() != null )
            {
                res.add( i.getIp() );
            }
        }
        return res;
    }
}