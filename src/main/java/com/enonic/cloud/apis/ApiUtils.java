package com.enonic.cloud.apis;

import javax.ws.rs.WebApplicationException;

public class ApiUtils
{
    public static RuntimeException formatWebApplicationException( WebApplicationException e, String prefix ) {
        return new RuntimeException( String.format( "%s: Error [code: %d]: %s", prefix, e.getResponse().getStatus(), e.getResponse().readEntity( String.class )) );
    }
}
