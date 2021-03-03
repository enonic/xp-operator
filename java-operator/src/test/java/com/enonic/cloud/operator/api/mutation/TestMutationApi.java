package com.enonic.cloud.operator.api.mutation;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.enonic.cloud.kubernetes.Searchers;

public class TestMutationApi
    extends MutationApi
{
    public TestMutationApi( ObjectMapper mapper, Searchers searchers )
    {
        super();
        this.mapper = mapper;
        this.searchers = searchers;
        this.lbServiceIpProducer = new TestLbServiceIpProducer();
    }
}
