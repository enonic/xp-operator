package com.enonic.kubernetes.operator.domain;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.kubernetes.apis.cloudflare.DnsRecordServiceWrapper;
import com.enonic.kubernetes.apis.cloudflare.service.model.DnsRecord;
import com.enonic.kubernetes.apis.cloudflare.service.model.ImmutableDnsRecord;
import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.common.functions.RunnableListExecutor;

public class CNameDnsRecordManager
//    implements DnsRecordManager
{
//    private final Logger LOG = LoggerFactory.getLogger( CNameDnsRecordManager.class );
//
//    DnsRecordServiceWrapper dnsRecordService;
//
//    RunnableListExecutor runnableListExecutor;
//
//    String cnameDomain;
//
//    DomainConfig config;
//
//    Domain domain;
//
//    public CNameDnsRecordManager( final DnsRecordServiceWrapper dnsRecordService, final RunnableListExecutor runnableListExecutor,
//                                  final String cnameDomain, final DomainConfig config, final Domain domain )
//    {
//        this.dnsRecordService = dnsRecordService;
//        this.runnableListExecutor = runnableListExecutor;
//        this.cnameDomain = cnameDomain;
//        this.config = config;
//        this.domain = domain;
//    }
//
//    @Override
//    public String syncRecords( final List<DnsRecord> records )
//    {
//        List<DnsRecord> cnameRecords = records.stream().filter( r -> "CNAME".equals( r.type() ) ).collect( Collectors.toList() );
//        final List<String> currentRecordCNames = cnameRecords.stream().map( DnsRecord::content ).collect( Collectors.toList() );
//
//        // Remove all records that do not have the current IPs the lb has
//        aRecords.stream().filter( r -> !ips.contains( r.content() ) ).forEach( this::deleteRecord);
//
//        if(!currentRecordCNames.contains( cnameDomain ))
//        {
//            this.addRecord( ImmutableDnsRecord.builder()
//                                .zone_id( config.zoneId() )
//                                .name( domain.getSpec().getHost() )
//                                .ttl( domain.getSpec().getDnsTTL() )
//                                .content( cnameDomain )
//                                .type( "CNAME" )
//                                .proxied( domain.getSpec().getCdn() )
//                                .build() );
//        }
//
//        aRecords.stream().filter( r -> !toRemove.contains( r ) ).forEach( r -> {
//            if ( !r.ttl().equals( domain.getSpec().getDnsTTL() ) || r.proxied() != domain.getSpec().getCdn() )
//            {
//                this.updateRecord( ImmutableDnsRecord.builder()
//                                       .from( r )
//                                       .ttl( domain.getSpec().getDnsTTL() )
//                                       .proxied( domain.getSpec().getCdn() )
//                                       .build() );
//            }
//        } );
//
//
//    }
//
//
//    @Override
//    public String deleteRecords( final List<DnsRecord> records )
//    {
//        return records.stream()
////            .filter( r -> !cnameDomain.equals( r.content() ) )
//            .map( this::deleteRecord )
//            .filter( Objects::nonNull )
//            .collect( Collectors.joining( "," ) );
//    }
//
//
//    @Override
//    protected Logger log()
//    {
//        return LOG;
//    }


}



