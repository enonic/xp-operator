package com.enonic.cloud.operator.dns.functions;

import java.util.Map;

import org.immutables.value.Value;

import com.enonic.cloud.common.annotations.Params;
import com.enonic.cloud.operator.dns.model.Domain;
import com.enonic.cloud.operator.dns.model.Record;

@Value.Immutable
@Params
public interface DomainSyncerParams
{
    Map<Domain, Record> currentRecords();

    Map<Domain, Record> markedForDeletion();
}
