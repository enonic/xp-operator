package com.enonic.cloud.apis.xp.service;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class CustomRestHeaderFilter
    implements ClientRequestFilter
{
    private final String name;

    private final String value;

    public CustomRestHeaderFilter( String name, String value )
    {
        this.name = name;
        this.value = value;
    }

    @Override
    public void filter( ClientRequestContext requestContext )
    {
        requestContext.getHeaders().add( name, value );
    }
}
