package com.enonic.ec.kubernetes.operator.helm;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ObjectMapperProducer
{
    @Produces
    @Singleton
    @Named("yaml")
    public ObjectMapper produceObjectMapper()
    {
        return new ObjectMapper( new YAMLFactory() );
    }
}
