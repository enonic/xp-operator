package com.enonic.ec.kubernetes.operator.dns.commands;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.enonic.ec.kubernetes.common.commands.CombinedCommandBuilder;
import com.enonic.ec.kubernetes.common.commands.ImmutableCombinedCommand;
import com.enonic.ec.kubernetes.dns.cloudflare.model.DnsRecord;

@Value.Immutable
public abstract class DnsApplyIngress
    extends DnsCommand
    implements CombinedCommandBuilder
{
    @Override
    public void addCommands( final ImmutableCombinedCommand.Builder commandBuilder )
    {
        if ( !areManaged() )
        {
            return;
        }

        // Create a new record
        if ( records().size() == 0 )
        {
            create( commandBuilder, "TXT", Integer.MAX_VALUE, heritageRecord(), null );
        }

        // Create/Update records
        List<DnsRecord> existingRecords = records().stream().filter( r -> r.type().equals( "A" ) ).collect( Collectors.toList() );
        List<String> existingIps = existingRecords.stream().map( r -> r.content() ).collect( Collectors.toList() );

        // If there is only 1 A record
        if ( existingRecords.size() == ips().size() && existingRecords.size() > 1 )
        {
            DnsRecord record = existingRecords.get( 0 );
            if ( !record.content().equals( ips().get( 0 ) ) )
            {
                update( commandBuilder, record.id(), record.type(), record.ttl(), ips().get( 0 ) );
            }
            return;
        }

        for ( String ip : ips() )
        {
            if ( !existingIps.contains( ip ) )
            {
                create( commandBuilder, "A", ttl(), ip, false );
            }
        }
        for ( DnsRecord record : existingRecords )
        {
            if ( !ips().contains( record.content() ) )
            {
                delete( commandBuilder, record );
            }
        }
    }
}
