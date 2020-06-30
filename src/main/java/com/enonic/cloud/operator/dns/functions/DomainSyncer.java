package com.enonic.cloud.operator.dns.functions;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cloud.apis.cloudflare.CloudflareListParams;
import com.enonic.cloud.apis.cloudflare.CloudflareListParamsImpl;
import com.enonic.cloud.apis.cloudflare.service.model.DnsRecord;
import com.enonic.cloud.apis.cloudflare.service.model.ImmutableDnsRecord;
import com.enonic.cloud.operator.dns.model.Domain;
import com.enonic.cloud.operator.dns.model.Record;


@Singleton
public class DomainSyncer
    implements Function<DomainSyncerParams, List<Runnable>>
{
    private static final Logger log = LoggerFactory.getLogger( DomainSyncer.class );

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    Function<CloudflareListParams, List<DnsRecord>> cfList;

    @Inject
    @Named("create")
    Function<DnsRecord, Runnable> cfCreate;

    @Inject
    @Named("delete")
    Function<DnsRecord, Runnable> cfDelete;

    @Override
    public List<Runnable> apply( final DomainSyncerParams params )
    {
        List<Runnable> res = new LinkedList<>();
        for ( Map.Entry<Domain, Record> e : params.currentRecords().entrySet() )
        {
            applyRecords( res, e.getKey(), e.getValue() );
        }

        for ( Domain d : params.markedForDeletion().keySet() )
        {
            if ( !params.currentRecords().containsKey( d ) )
            {
                deleteAllRecords( res, d, params.markedForDeletion().get( d ) );
            }
        }
        return res;
    }

    private void applyRecords( final List<Runnable> commands, final Domain domain, final Record record )
    {
        List<DnsRecord> records = getRecords( domain );
        Optional<DnsRecord> heritageRecord = heritageRecord( records, record );
        if ( heritageRecord.isEmpty() && records.size() > 0 )
        {
            log.warn( String.format( "Skipping domain '%s' because heritage records do not match", domain.domain() ) );
            return;
        }

        // Create heritage record if missing
        if ( heritageRecord.isEmpty() )
        {
            commands.add( cfCreate.apply( ImmutableDnsRecord.builder().
                zone_id( domain.zoneId() ).
                name( domain.domain() ).
                ttl( Integer.MAX_VALUE ).
                content( record.heritage() ).
                type( "TXT" ).
                build() ) );
        }

        // Figure out what to add and remove
        List<DnsRecord> aRecords = records.stream().filter( r -> r.type().equals( "A" ) ).collect( Collectors.toList() );
        Set<String> currentIps = aRecords.stream().map( DnsRecord::content ).collect( Collectors.toSet() );

        Set<String> toAdd = new HashSet<>( record.ips() );
        toAdd.removeAll( currentIps );

        Set<String> toRemove = new HashSet<>( currentIps );
        toRemove.removeAll( record.ips() );

        // Add missing records
        toAdd.stream().map( ip -> ImmutableDnsRecord.builder().
            zone_id( domain.zoneId() ).
            name( domain.domain() ).
            ttl( record.ttl() ).
            content( ip ).
            type( "A" ).
            proxied( record.cdn() ).build() ).
            map( cfCreate ).
            forEach( commands::add );

        // Delete old records
        aRecords.stream().
            filter( r -> toRemove.contains( r.content() ) ).
            map( cfDelete ).
            forEach( commands::add );
    }

    private void deleteAllRecords( final List<Runnable> commands, final Domain domain, final Record record )
    {
        List<DnsRecord> records = getRecords( domain );
        heritageRecord( records, record ).ifPresent( r -> records.stream().map( cfDelete ).forEach( commands::add ) );
    }

    private List<DnsRecord> getRecords( final Domain domain )
    {
        return cfList.apply( CloudflareListParamsImpl.of( domain.zoneId(), domain.domain(), null ) );
    }

    private Optional<DnsRecord> heritageRecord( List<DnsRecord> records, final Record record )
    {
        return records.stream().
            filter( r -> r.type().equals( "TXT" ) ).
            filter( r -> r.content().equals( record.heritage() ) ).
            findFirst();
    }
}
