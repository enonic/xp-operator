package com.enonic.ec.kubernetes.operator.dns.commands;

import java.util.List;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.ec.kubernetes.common.Configuration;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.dns.cloudflare.DnsRecords;
import com.enonic.ec.kubernetes.dns.cloudflare.model.DnsRecord;
import com.enonic.ec.kubernetes.dns.cloudflare.model.ImmutableDnsRecord;
import com.enonic.ec.kubernetes.operator.dns.AllowedDomain;
import com.enonic.ec.kubernetes.operator.dns.DnsIngress;
import com.enonic.ec.kubernetes.operator.dns.cloudflare.ImmutableDnsCreate;
import com.enonic.ec.kubernetes.operator.dns.cloudflare.ImmutableDnsDelete;
import com.enonic.ec.kubernetes.operator.dns.cloudflare.ImmutableDnsUpdate;

public abstract class DnsCommand
    extends Configuration
{
    private final static Logger log = LoggerFactory.getLogger( DnsCommand.class );

    protected abstract DnsRecords dnsRecords();

    protected abstract DnsIngress dnsIngress();

    @Value.Derived
    protected String zoneId()
    {
        return dnsIngress().allowedDomain().get().zoneId();
    }

    @Value.Derived
    protected String subDomain()
    {
        return dnsIngress().subDomain().get();
    }

    @Value.Derived
    protected List<String> ips()
    {
        return dnsIngress().ips();
    }

    @Value.Derived
    protected Integer ttl()
    {
        return Integer.parseInt( dnsIngress().ttl() );
    }

    @Value.Derived
    protected List<DnsRecord> records()
    {
        AllowedDomain allowedDomain = dnsIngress().allowedDomain().get();
        return dnsRecords().list( allowedDomain.zoneId(), dnsIngress().subDomain().get(), null ).result();
    }

    @Value.Derived
    protected boolean areManaged()
    {
        if ( records().size() == 0 )
        {
            return true;
        }
        return records().stream().
            anyMatch( r -> r.type().equals( "TXT" ) && r.content().equals( heritageRecord() ) );
    }

    @Value.Derived
    protected String heritageRecord()
    {
        return "\"heritage=ec-operator\"";
    }

    protected void create( final ImmutableCombinedCommand.Builder commandBuilder, final String type, final Integer ttl,
                           final String content, final Boolean proxied )
    {
        commandBuilder.addCommand( ImmutableDnsCreate.builder().
            dnsRecordsService( dnsRecords() ).
            zoneId( zoneId() ).
            dnsRecord( ImmutableDnsRecord.builder().
                name( subDomain() ).
                type( type ).
                content( content ).
                proxied( proxied ).
                ttl( ttl ).
                build() ).
            build() );
    }

    protected void delete( final ImmutableCombinedCommand.Builder commandBuilder, final DnsRecord record )
    {
        commandBuilder.addCommand( ImmutableDnsDelete.builder().
            dnsRecordsService( dnsRecords() ).
            zoneId( zoneId() ).
            dnsRecord( record ).
            build() );
    }

    protected void update( final ImmutableCombinedCommand.Builder commandBuilder, final String id, final String type, final Integer ttl,
                           final String content )
    {
        commandBuilder.addCommand( ImmutableDnsUpdate.builder().
            dnsRecordsService( dnsRecords() ).
            zoneId( zoneId() ).
            dnsRecord( ImmutableDnsRecord.builder().
                id( id ).
                name( subDomain() ).
                type( type ).
                content( content ).
                proxied( false ).
                ttl( ttl ).
                build() ).
            build() );
    }
}
