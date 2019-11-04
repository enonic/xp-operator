package com.enonic.ec.kubernetes.api;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class JacksonModules
    implements ObjectMapperCustomizer
{
    public void customize( ObjectMapper mapper )
    {
        // So jackson serializes Optional correctly
        mapper.registerModule( new Jdk8Module() );
    }
}
