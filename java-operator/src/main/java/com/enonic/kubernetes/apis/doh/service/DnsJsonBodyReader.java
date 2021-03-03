package com.enonic.kubernetes.apis.doh.service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
@Consumes({"application/dns-json"})
public class DnsJsonBodyReader
    implements MessageBodyReader
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean isReadable( final Class aClass, final Type type, final Annotation[] annotations, final MediaType mediaType )
    {
        return mediaType.getType().equals( "application" ) && mediaType.getSubtype().equals( "dns-json" );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object readFrom( final Class aClass, final Type type, final Annotation[] annotations, final MediaType mediaType,
                            final MultivaluedMap multivaluedMap, final InputStream inputStream )
        throws IOException, WebApplicationException
    {
        return objectMapper.readValue( inputStream, aClass );
    }
}
