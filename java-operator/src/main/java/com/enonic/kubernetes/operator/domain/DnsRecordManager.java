package com.enonic.kubernetes.operator.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.kubernetes.apis.cloudflare.DnsRecordServiceWrapper;
import com.enonic.kubernetes.apis.cloudflare.service.model.DnsRecord;
import com.enonic.kubernetes.apis.cloudflare.service.model.ImmutableDnsRecord;
import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.common.functions.RunnableListExecutor;

public final class DnsRecordManager
{
    private final Logger LOG = LoggerFactory.getLogger( DnsRecordManager.class );

    private final DnsRecordServiceWrapper dnsRecordService;

    private final RunnableListExecutor runnableListExecutor;

    private final DomainConfig config;

    private final Domain domain;

    protected DnsRecordManager( DnsRecordServiceWrapper dnsRecordService, RunnableListExecutor runnableListExecutor, DomainConfig config,
                                Domain domain )
    {
        this.dnsRecordService = dnsRecordService;
        this.runnableListExecutor = runnableListExecutor;
        this.config = config;
        this.domain = domain;
    }

    public String syncRecords( final List<DnsRecord> records, final List<String> lbAddresses, final String type, final String clusterId )
    {
        final List<String> errorMessages = new ArrayList<>();

        final List<DnsRecord> aRecords = records.stream().filter( r -> type.equals( r.type() ) ).collect( Collectors.toList() );
        final List<String> currentRecordIps = aRecords.stream().map( DnsRecord::content ).collect( Collectors.toList() );

        if ( getHeritageRecord( records, clusterId ) == null )
        {
            errorMessages.add( this.addHeritageRecord( clusterId ));
        }

        // Remove all records that do not have the current IPs the lb has
        final List<DnsRecord> recordsToDelete =
            aRecords.stream().filter( r -> !lbAddresses.contains( r.content() ) ).collect( Collectors.toList() );

        recordsToDelete.stream().map( this::deleteRecord ).forEach( errorMessages::add );

        lbAddresses.stream()
            .filter( lbAddress -> !currentRecordIps.contains( lbAddress ) )
            .map( ip -> this.addRecord( ImmutableDnsRecord.builder()
                                            .zone_id( config.zoneId() )
                                            .name( domain.getSpec().getHost() )
                                            .ttl( domain.getSpec().getDnsTTL() )
                                            .content( ip )
                                            .type( type )
                                            .proxied( domain.getSpec().getCdn() )
                                            .build() ) )
            .forEach( errorMessages::add );

        // Update existing records if needed
        aRecords.stream()
            .filter( r -> !recordsToDelete.contains( r ) )
            .filter( r -> !r.ttl().equals( domain.getSpec().getDnsTTL() ) || r.proxied() != domain.getSpec().getCdn() )
            .map( r -> this.updateRecord(
                ImmutableDnsRecord.builder().from( r ).ttl( domain.getSpec().getDnsTTL() ).proxied( domain.getSpec().getCdn() ).build() ) )
            .forEach( errorMessages::add );

        return errorMessages.stream().filter( Objects::nonNull ).collect( Collectors.joining( "," ) );
    }

    public String deleteRecords( final List<DnsRecord> records )
    {
        return records.stream().map( this::deleteRecord ).filter( Objects::nonNull ).collect( Collectors.joining( "," ) );
    }

    public DnsRecord getHeritageRecord( final List<DnsRecord> records, final String clusterId )
    {
        // Get heritage record
        return records.stream()
            .filter( r -> "TXT".equals( r.type() ) )
            .filter( r -> getHeritageRecordContent( clusterId ).equals( r.content() ) )
            .findFirst()
            .orElse( null );
    }


    private String deleteRecord( DnsRecord record )
    {
        return handleDnsOperation( record, dnsRecordService::delete, "delete" );
    }

    private String updateRecord( DnsRecord record )
    {
        return handleDnsOperation( record, dnsRecordService::update, "update" );
    }

    private String addRecord( DnsRecord record )
    {
        return handleDnsOperation( record, dnsRecordService::create, "create" );
    }

    private String addHeritageRecord( final String clusterId )
    {
        return handleDnsOperation( ImmutableDnsRecord.builder()
                                       .zone_id( config.zoneId() )
                                       .name( domain.getSpec().getHost() )
                                       .ttl( domain.getSpec().getDnsTTL() )
                                       .type( "TXT" )
                                       .content( getHeritageRecordContent( clusterId ) )
                                       .build(), dnsRecordService::create, "create heritage" );
    }

    private String getHeritageRecordContent( final String clusterId )
    {
        return "heritage=xp-operator,id=" + clusterId;
    }


    private String handleDnsOperation( final DnsRecord record, final Function<DnsRecord, Runnable> operation,
                                       final String operationDescription )
    {
        try
        {
            runnableListExecutor.apply( List.of(operation.apply( record )));
        }
        catch ( Exception e )
        {
            final String errorMessage =
                String.format( "Failed to %s record: %s with type: %s and content: %s due to: %s.", operationDescription, record.name(),
                               record.type(), record.content(), e.getMessage() );

            LOG.error( errorMessage, e );

            return errorMessage;
        }

        return null;
    }
}
