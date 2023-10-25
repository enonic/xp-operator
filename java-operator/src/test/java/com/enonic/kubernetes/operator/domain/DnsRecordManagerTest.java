package com.enonic.kubernetes.operator.domain;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.enonic.kubernetes.apis.cloudflare.DnsRecordServiceWrapper;
import com.enonic.kubernetes.apis.cloudflare.service.model.DnsRecord;
import com.enonic.kubernetes.apis.cloudflare.service.model.ImmutableDnsRecord;
import com.enonic.kubernetes.client.v1.domain.Domain;
import com.enonic.kubernetes.common.functions.RunnableListExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DnsRecordManagerTest {
//    private DnsRecordManager dnsRecordManager;
//    @Mock
//    private DnsRecordServiceWrapper dnsRecordService;
//    @Mock
//    private RunnableListExecutor runnableListExecutor;
//    @Mock
//    private DomainConfig config;
//    @Mock
//    private Domain domain;
//
//    @BeforeEach
//    void setUp() {
//        dnsRecordManager = new DnsRecordManager(dnsRecordService, runnableListExecutor, config, domain);
//    }
//
//    @Test
//    void testSyncRecords() {
//        // Mock your data and behavior
//        List<DnsRecord> records = new ArrayList<>();
//        List<String> lbAdresses = new ArrayList<>();
//        String type = "A";
//        String clusterId = "cluster123";
//
//        when(dnsRecordService.create(any())).thenReturn(null);
//        when(dnsRecordService.delete(any())).thenReturn(null);
//        when(dnsRecordService.update(any())).thenReturn(null);
//
//        // Test the method
//        String result = dnsRecordManager.syncRecords(records, lbAdresses, type, clusterId);
//
//        // Assert the result
//        assertNull( result);
//    }
//
//    @Test
//    void testDeleteRecords() {
//        // Mock your data and behavior
//        List<DnsRecord> records = new ArrayList<>();
//
//        when(dnsRecordService.delete(any())).thenReturn(null);
//
//        // Test the method
//        String result = dnsRecordManager.deleteRecords(records);
//
//        // Assert the result
//        assertNull(result);
//    }
//
//    @Test
//    void testGetHeritageRecord() {
//        // Mock your data and behavior
//        List<DnsRecord> records = new ArrayList<>();
//        String clusterId = "cluster123";
//
//        // Set up a record with the expected content
//        DnsRecord expectedRecord = ImmutableDnsRecord.builder()
//            .type("TXT")
//            .content("heritage=xp-operator,id=" + clusterId)
//            .build();
//        records.add(expectedRecord);
//
//        // Test the method
//        DnsRecord result = dnsRecordManager.getHeritageRecord(records, clusterId);
//
//        // Assert the result
//        assertEquals(expectedRecord, result);
//    }

    // Add more test cases to cover other methods and edge cases
}
