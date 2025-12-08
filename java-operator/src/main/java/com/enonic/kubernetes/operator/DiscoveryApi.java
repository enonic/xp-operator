package com.enonic.kubernetes.operator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/apis")
public class DiscoveryApi
{
    @GET
    @Produces("application/json")
    public Response get()
    {
        return Response.status( Response.Status.NOT_ACCEPTABLE ).build();
    }
}
