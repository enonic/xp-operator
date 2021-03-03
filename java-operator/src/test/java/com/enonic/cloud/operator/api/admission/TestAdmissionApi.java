package com.enonic.cloud.operator.api.admission;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.enonic.cloud.kubernetes.Searchers;

public class TestAdmissionApi
    extends AdmissionApi
{
    public TestAdmissionApi( ObjectMapper mapper, Searchers searchers )
    {
        super();
        this.mapper = mapper;
        this.searchers = searchers;
    }
}
