package com.enonic.cloud.operator.api.mutation;

import java.util.Collections;
import java.util.List;

import com.enonic.cloud.operator.domain.LbServiceIpProducer;

public class TestLbServiceIpProducer
    extends LbServiceIpProducer
{
    public TestLbServiceIpProducer()
    {
        super();
    }

    @Override
    public List<String> get()
    {
        return Collections.singletonList( "1.2.3.4" );
    }
}
