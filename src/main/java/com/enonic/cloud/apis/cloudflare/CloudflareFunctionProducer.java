package com.enonic.cloud.apis.cloudflare;

import java.util.List;
import java.util.function.Function;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.enonic.cloud.apis.cloudflare.service.DnsRecordService;
import com.enonic.cloud.apis.cloudflare.service.model.DnsRecord;

@SuppressWarnings("CdiInjectionPointsInspection")
public class CloudflareFunctionProducer
{
    @Produces
    @Singleton
    public Function<CloudflareListParams, List<DnsRecord>> producerListFunction( @RestClient DnsRecordService service )
    {
        return ( params ) -> service.list( params.zoneId(), params.name(), params.type() ).result();
    }

    @Produces
    @Singleton
    @Named("create")
    public Function<DnsRecord, Runnable> producerCreateFunction( @RestClient DnsRecordService service )
    {
        return ( dnsRecord ) -> ImmutableCloudflareCommand.builder().
            action( "CREATE" ).
            dnsRecord( dnsRecord ).
            wrappedRunnable( () -> service.create( dnsRecord.zone_id(), dnsRecord ) ).
            build();
    }

    @Produces
    @Singleton
    @Named("update")
    public Function<DnsRecord, Runnable> producerUpdateFunction( @RestClient DnsRecordService service )
    {
        return ( dnsRecord ) -> ImmutableCloudflareCommand.builder().
            action( "UPDATE" ).
            dnsRecord( dnsRecord ).
            wrappedRunnable( () -> service.update( dnsRecord.zone_id(), dnsRecord.id(), dnsRecord ) ).
            build();
    }

    @Produces
    @Singleton
    @Named("delete")
    public Function<DnsRecord, Runnable> producerDeleteFunction( @RestClient DnsRecordService service )
    {
        return ( dnsRecord ) -> ImmutableCloudflareCommand.builder().
            action( "DELETE" ).
            dnsRecord( dnsRecord ).
            wrappedRunnable( () -> service.delete( dnsRecord.zone_id(), dnsRecord.id() ) ).
            build();
    }
}
