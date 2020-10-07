package com.enonic.cloud.operator.api.admission;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.enonic.cloud.kubernetes.Searchers;
import com.enonic.cloud.operator.api.mutation.MutationApi;

public class TestMutationApi
    extends MutationApi
{
    public TestMutationApi( ObjectMapper mapper, Searchers searchers )
    {
        super();
        this.mapper = mapper;
        this.searchers = searchers;
    }
}
