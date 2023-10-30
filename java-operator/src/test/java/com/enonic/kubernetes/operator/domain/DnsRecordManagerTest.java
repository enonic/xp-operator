package com.enonic.kubernetes.operator.domain;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.enonic.kubernetes.apis.cloudflare.DnsRecordServiceWrapper;
import com.enonic.kubernetes.apis.cloudflare.service.model.DnsRecord;
import com.enonic.kubernetes.apis.cloudflare.service.model.ImmutableDnsRecord;
import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.client.v1.domain.DomainSpec;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DnsRecordManagerTest
{
    private DnsRecordManager dnsRecordManager;

    @Mock
    private DnsRecordServiceWrapper dnsRecordService;


    @Mock
    private DomainConfig config;

    @Mock
    private Domain domain;

    @BeforeEach
    void setUp()
    {
        dnsRecordManager = new DnsRecordManager( dnsRecordService, config, domain );
    }

    @Test
    void testSyncRecords_createHeritageRecordFailed()
    {
        mockConfig();
        mockDomain();

        final Runnable runnable = mock( Runnable.class );
        when( dnsRecordService.create( any() ) ).thenReturn( runnable );
        doThrow( new RuntimeException( "custom error" ) ).when( runnable ).run();

        final String result = dnsRecordManager.syncRecords( new ArrayList<>(), new ArrayList<>(), "A", "cluster123" );

        assertEquals(
            "Failed to create heritage record: host123 with type: TXT and content: heritage=xp-operator,id=cluster123 due to: custom error.",
            result );
    }

    @Test
    void testSyncRecords_deleteRecordFailed()
    {
        mockConfig();
        mockDomain();

        final Runnable deletionRunnable = mock( Runnable.class );
        when( dnsRecordService.create( any() ) ).thenReturn( mock( Runnable.class ) );
        when( dnsRecordService.delete( any() ) ).thenReturn( deletionRunnable );

        final DnsRecord recordToDelete =
            ImmutableDnsRecord.builder().name( "recordToDelete1" ).type( "A" ).content( "not_supported_ip" ).build();

        doThrow( new RuntimeException( "custom error" ) ).when( deletionRunnable ).run();

        final String result = dnsRecordManager.syncRecords( List.of(recordToDelete), new ArrayList<>(), "A", "cluster123" );

        assertEquals(
            "Failed to delete record: recordToDelete1 with type: A and content: not_supported_ip due to: custom error.",
            result );
    }

    @Test
    void testSyncRecords_updateRecordFailed()
    {
        mockConfig();
        mockDomain();

        final Runnable updateRunnable = mock( Runnable.class );
        when( dnsRecordService.create( any() ) ).thenReturn( mock( Runnable.class ) );
        when( dnsRecordService.delete( any() ) ).thenReturn( mock( Runnable.class ) );
        when( dnsRecordService.update( any() ) ).thenReturn( updateRunnable );

        final DnsRecord recordToUpdate =
            ImmutableDnsRecord.builder().name( "recordToUpdate1" ).type( "A" ).content( "1.2.3.4" ).build();

        doThrow( new RuntimeException( "custom error" ) ).when( updateRunnable ).run();

        final String result = dnsRecordManager.syncRecords( List.of(recordToUpdate), List.of(recordToUpdate.content()), "A", "cluster123" );

        assertEquals(
            "Failed to update record: recordToUpdate1 with type: A and content: 1.2.3.4 due to: custom error.",
            result );
    }

    @Test
    void testSyncRecords()
    {
        mockConfig();
        mockDomain();

        // Mock your data
        final DnsRecord recordToDelete1 =
            ImmutableDnsRecord.builder().name( "recordToDelete1" ).type( "A" ).content( "not_supported_ip" ).build();
        final DnsRecord recordToDelete2 =
            ImmutableDnsRecord.builder().name( "recordToDelete2" ).type( "CNAME" ).content( "not_supported_cname" ).build();
        final DnsRecord recordToUpdate1 =
            ImmutableDnsRecord.builder().name( "recordToUpdate1" ).type( "A" ).content( "5.5.5.5" ).ttl( 123 ).build();
        final DnsRecord recordToUpdate2 =
            ImmutableDnsRecord.builder().name( "recordToUpdate2" ).type( "A" ).content( "6.6.6.6" ).ttl( 234 ).proxied( true ).build();
        final DnsRecord recordToIgnore =
            ImmutableDnsRecord.builder().name( "recordToIgnore" ).type( "A" ).content( "7.7.7.7" ).ttl( 123 ).proxied( true ).build();

        final List<DnsRecord> records = List.of( recordToDelete1, recordToDelete2, recordToUpdate1, recordToUpdate2, recordToIgnore );
        final List<String> lbAddresses =
            List.of( "1.2.3.4", recordToUpdate1.content(), recordToUpdate2.content(), recordToIgnore.content() );

        final String type = "A";
        final String clusterId = "cluster123";

        final ArgumentCaptor<DnsRecord> dnsRecordArgumentCaptor = ArgumentCaptor.forClass( DnsRecord.class );

        final Runnable createRecordCommand = mock( Runnable.class );
        final Runnable deleteRecordCommand = mock( Runnable.class );
        final Runnable updateRecordCommand = mock( Runnable.class );

        when( dnsRecordService.create( dnsRecordArgumentCaptor.capture() ) ).thenReturn( createRecordCommand );
        when( dnsRecordService.delete( dnsRecordArgumentCaptor.capture() ) ).thenReturn( deleteRecordCommand );
        when( dnsRecordService.update( dnsRecordArgumentCaptor.capture() ) ).thenReturn( updateRecordCommand );

        final String result = dnsRecordManager.syncRecords( records, lbAddresses, type, clusterId );

        verify( createRecordCommand, Mockito.times( 2 ) ).run();
        verify( deleteRecordCommand, Mockito.times( 2 ) ).run();
        verify( updateRecordCommand, Mockito.times( 2 ) ).run();

        // Assert a heritage record was created
        assertEquals( ImmutableDnsRecord.builder()
                          .type( "TXT" )
                          .name( "host123" )
                          .content( "heritage=xp-operator,id=cluster123" )
                          .ttl( 123 )
                          .zone_id( "zone123" )
                          .build(), dnsRecordArgumentCaptor.getAllValues().get( 0 ) );

        // Assert a record was deleted
        assertEquals( recordToDelete1, dnsRecordArgumentCaptor.getAllValues().get( 1 ) );
        assertEquals( recordToDelete2, dnsRecordArgumentCaptor.getAllValues().get( 2 ) );

        // Assert a record was created
        assertEquals( ImmutableDnsRecord.builder()
                          .type( "A" )
                          .name( "host123" )
                          .content( "1.2.3.4" )
                          .ttl( 123 )
                          .proxied( true )
                          .zone_id( "zone123" )
                          .build(), dnsRecordArgumentCaptor.getAllValues().get( 3 ) );

        // Assert records were updated
        assertEquals( ImmutableDnsRecord.builder().from( recordToUpdate1 ).proxied( true ).build(),
                      dnsRecordArgumentCaptor.getAllValues().get( 4 ) );
        assertEquals( ImmutableDnsRecord.builder().from( recordToUpdate2 ).ttl( 123 ).proxied( true ).build(),
                      dnsRecordArgumentCaptor.getAllValues().get( 5 ) );

        // Assert no errors
        assertEquals( "", result );
    }

    @Test
    void testDeleteRecords()
    {
        final DnsRecord recordToDelete1 = ImmutableDnsRecord.builder().name( "recordToDelete1" ).type( "A" ).content( "1.2.3.4" ).build();
        final DnsRecord recordToDelete2 = ImmutableDnsRecord.builder().name( "recordToDelete2" ).type( "CNAME" ).content( "cname" ).build();
        final DnsRecord recordToDelete3 =
            ImmutableDnsRecord.builder().name( "recordToDelete3" ).type( "TXT" ).content( "txt_content" ).build();

        final ArgumentCaptor<DnsRecord> dnsRecordArgumentCaptor = ArgumentCaptor.forClass( DnsRecord.class );

        when( dnsRecordService.delete( dnsRecordArgumentCaptor.capture() ) ).thenReturn( mock( Runnable.class ) );

        // Test the method
        String result = dnsRecordManager.deleteRecords( List.of( recordToDelete1, recordToDelete2, recordToDelete3 ) );

        // Assert the result
        assertEquals( recordToDelete1, dnsRecordArgumentCaptor.getAllValues().get( 0 ) );
        assertEquals( recordToDelete2, dnsRecordArgumentCaptor.getAllValues().get( 1 ) );
        assertEquals( recordToDelete3, dnsRecordArgumentCaptor.getAllValues().get( 2 ) );

        assertEquals( "", result );
    }

    @Test
    void testGetHeritageRecord()
    {
        final String clusterId = "cluster123";

        final DnsRecord expectedRecord =
            ImmutableDnsRecord.builder().name( "heritage" ).type( "TXT" ).content( "heritage=xp-operator,id=" + clusterId ).build();
        final DnsRecord unexpectedRecord =
            ImmutableDnsRecord.builder().name( "name" ).type( "TXT" ).content( "heritage=xp-operator,id=123" ).build();

        // Test the method
        final DnsRecord result = dnsRecordManager.getHeritageRecord( List.of( expectedRecord, unexpectedRecord ), clusterId );

        // Assert the result
        assertEquals( expectedRecord, result );
    }

    private void mockConfig()
    {
        when( config.zoneId() ).thenReturn( "zone123" );
        when( config.domain() ).thenReturn( "domain123" );
    }

    private void mockDomain()
    {
        final DomainSpec domainSpec = mock( DomainSpec.class );

        when( domain.getSpec() ).thenReturn( domainSpec );

        when( domainSpec.getHost() ).thenReturn( "host123" );
        when( domainSpec.getDnsTTL() ).thenReturn( 123 );
        when( domainSpec.getCdn() ).thenReturn( true );
    }
}
