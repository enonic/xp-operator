package com.enonic.cloud.operator.dns;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.runtime.StartupEvent;

import com.enonic.cloud.common.functions.RunnableListExecutor;
import com.enonic.cloud.kubernetes.InformerSearcher;
import com.enonic.cloud.operator.InformerEventHandler;
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

import static com.enonic.cloud.common.Configuration.cfgStr;


@ApplicationScoped
public class OperatorDns
    extends InformerEventHandler<Ingress>
{
    private final IngressEnabledHosts ingressEnabledHosts = new IngressEnabledHosts();

    @Inject
    SharedIndexInformer<Ingress> ingressSharedIndexInformer;

    @Inject
    InformerSearcher<Ingress> ingressInformerSearcher;

    @ConfigProperty(name = "dns.enabled")
    Boolean enabled;

    @ConfigProperty(name = "dns.allowedDomains.keys")
    List<String> allowedDomainKeys;

    @Inject
    DomainSyncer domainSyncer;

    @Inject
    RunnableListExecutor runnableListExecutor;

    private DomainFromIngress domainFromIngress;

    private DomainMapper domainMapper;

    void onStartup( @Observes StartupEvent _ev )
    {
        if ( !enabled )
        {
            // If DNS is disabled, do not start listener
            return;
        }

        // Collect allowed domains
        final List<Domain> allowedDomains = allowedDomainKeys.stream().
            map( k -> DomainImpl.of( cfgStr( "dns.domain." + k ), cfgStr( "dns.zoneId." + k ) ) ).
            collect( Collectors.toList() );

        // Setup helper functions
        domainFromIngress = DomainFromIngressImpl.of( allowedDomains );
        domainMapper = DomainMapperImpl.of( domainFromIngress );

        // Start listening to events
        listenToInformer( ingressSharedIndexInformer );
    }

    @Override
    public void onAdd( final Ingress newResource )
    {
        // Ignore because ingresses are never assigned ips instantly
    }

    @Override
    public void onUpdate( final Ingress oldResource, final Ingress newResource )
    {
        handle( newResource, false );
    }

    @Override
    public void onDelete( final Ingress oldResource, final boolean b )
    {
        handle( oldResource, true );
    }

    private void handle( final Ingress ingress, boolean deleted )
    {
        Set<Ingress> ingresses = ingressInformerSearcher.getStream().
            filter( RelevantIngressPredicateImpl.of( ingress, ingressEnabledHosts ) ).
            collect( Collectors.toSet() );

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
}