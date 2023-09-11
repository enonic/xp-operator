package com.enonic.kubernetes.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

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
