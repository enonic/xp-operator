package com.enonic.ec.kubernetes.operator.operators.dns.commands;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.operator.common.Configuration;
import com.enonic.ec.kubernetes.operator.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.operator.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.operator.common.info.Diff;
import com.enonic.ec.kubernetes.operator.operators.dns.cloudflare.DnsRecordService;
import com.enonic.ec.kubernetes.operator.operators.dns.cloudflare.model.DnsRecord;
import com.enonic.ec.kubernetes.operator.operators.dns.cloudflare.model.ImmutableDnsRecord;
import com.enonic.ec.kubernetes.operator.operators.dns.commands.rest.ImmutableDnsCreate;
import com.enonic.ec.kubernetes.operator.operators.dns.commands.rest.ImmutableDnsDelete;
import com.enonic.ec.kubernetes.operator.operators.dns.model.DiffDnsIngress;
import com.enonic.ec.kubernetes.operator.operators.dns.model.DiffDnsIngressDomains;
import com.enonic.ec.kubernetes.operator.operators.dns.model.DiffDnsIngressIps;
import com.enonic.ec.kubernetes.operator.operators.dns.model.DnsIngress;
import com.enonic.ec.kubernetes.operator.operators.dns.model.Domain;

@Value.Immutable
public abstract class DnsApplyIngress
    extends Configuration
    implements CombinedCommandBuilder
{
    protected abstract DnsRecordService dnsRecordService();

    protected abstract DiffDnsIngress diff();

    protected abstract String dnsId();

    @Value.Derived
    protected String heritageRecord()
    {
        return String.format( "heritage=ec-operator,id=%s", dnsId() );
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // I know that it's ok
    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        if ( !diff().newValue().map( DnsIngress::shouldModify ).orElse(
            diff().oldValue().map( DnsIngress::shouldModify ).orElse( false ) ) )
        {
            return;
        }

        for ( DiffDnsIngressDomains diffDomain : diff().diffDomains() )
        {
            if ( !diffDomain.shouldRemove() )
            {
                addOrModify( commandBuilder, diffDomain.newValue().get(), diff().newValue().get().ttl(), diff().diffIps() );
            }
            else
            {
                delete( commandBuilder, diffDomain.oldValue().get() );
            }
        }
    }

    private void addOrModify( final ImmutableCombinedCommand.Builder commandBuilder, final Domain domain, final Integer ttl,
                              final List<DiffDnsIngressIps> ipsChanged )
    {
        List<DnsRecord> records = dnsRecordService().list( domain.zoneId(), domain.domain(), null ).result();
        Optional<DnsRecord> heritageRecord = getHeritageRecord( records );

        if ( records.size() > 0 && heritageRecord.isEmpty() )
        {
            return;
        }

        if ( heritageRecord.isEmpty() )
        {
            commandBuilder.addCommand( ImmutableDnsCreate.builder().
                dnsRecordsService( dnsRecordService() ).
                zoneId( domain.zoneId() ).
                dnsRecord( ImmutableDnsRecord.builder().
                    name( domain.domain() ).
                    type( "TXT" ).
                    content( heritageRecord() ).
                    proxied( false ).
                    ttl( Integer.MAX_VALUE ).
                    build() ).
                build() );
        }

        List<String> dnsRecords =
            records.stream().filter( r -> r.type().equals( "A" ) ).map( DnsRecord::content ).collect( Collectors.toList() );
        ipsChanged.stream().
            filter( Diff::isNew ).
            filter( d -> !dnsRecords.contains( d.ip() ) ).
            forEach( diffIp -> commandBuilder.addCommand( ImmutableDnsCreate.builder().
                dnsRecordsService( dnsRecordService() ).
                zoneId( domain.zoneId() ).
                dnsRecord( ImmutableDnsRecord.builder().
                    name( domain.domain() ).
                    type( "A" ).
                    content( diffIp.ip() ).
                    proxied( false ).
                    ttl( ttl ).
                    build() ).
                build() ) );

        List<String> ipsToRemove =
            ipsChanged.stream().filter( Diff::shouldRemove ).map( DiffDnsIngressIps::ip ).collect( Collectors.toList() );
        records.stream().filter( r -> ipsToRemove.contains( r.content() ) ).forEach(
            r -> commandBuilder.addCommand( ImmutableDnsDelete.builder().
                dnsRecordsService( dnsRecordService() ).
                zoneId( domain.zoneId() ).
                dnsRecord( r ).
                build() ) );

    }

    private void delete( final ImmutableCombinedCommand.Builder commandBuilder, final Domain domain )
    {
        List<DnsRecord> records = dnsRecordService().list( domain.zoneId(), domain.domain(), null ).result();
        Optional<DnsRecord> heritageRecord = getHeritageRecord( records );
        if ( heritageRecord.isEmpty() )
        {
            return;
        }
        for ( DnsRecord record : records )
        {
            commandBuilder.addCommand( ImmutableDnsDelete.builder().
                dnsRecordsService( dnsRecordService() ).
                zoneId( domain.zoneId() ).
                dnsRecord( record ).
                build() );
        }
    }

    private Optional<DnsRecord> getHeritageRecord( List<DnsRecord> records )
    {
        if ( records.size() == 0 )
        {
            return Optional.empty();
        }
        return records.stream().
            filter( r -> r.type().equals( "TXT" ) && r.content().equals( heritageRecord() ) ).
            findFirst();
    }
}
